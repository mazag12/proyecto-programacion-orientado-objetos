package com.computototal.inventario.dao.jdbc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

public final class HsqlDatabase implements AutoCloseable {
    private static final List<String> ESQUEMA = List.of(
            "CREATE TABLE IF NOT EXISTS categoria ("
                    + "id VARCHAR(36) PRIMARY KEY, nombre VARCHAR(150) NOT NULL, descripcion VARCHAR(500) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS proveedor ("
                    + "id VARCHAR(36) PRIMARY KEY, nombre VARCHAR(150) NOT NULL, telefono VARCHAR(40) NOT NULL, "
                    + "correo_electronico VARCHAR(254) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS ubicacion ("
                    + "id VARCHAR(36) PRIMARY KEY, sede VARCHAR(150) NOT NULL, ambiente VARCHAR(150) NOT NULL, "
                    + "area VARCHAR(150) NOT NULL, piso VARCHAR(40) NOT NULL, es_almacen BOOLEAN NOT NULL)",
            "CREATE TABLE IF NOT EXISTS usuario ("
                    + "id VARCHAR(36) PRIMARY KEY, nombre_usuario VARCHAR(80) NOT NULL, "
                    + "nombre_usuario_normalizado VARCHAR(160) NOT NULL UNIQUE, "
                    + "hash_contrasena VARBINARY(128) NOT NULL, sal_contrasena VARBINARY(128) NOT NULL, "
                    + "rol VARCHAR(30) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS equipo ("
                    + "id VARCHAR(36) PRIMARY KEY, codigo VARCHAR(80) NOT NULL, "
                    + "codigo_normalizado VARCHAR(160) NOT NULL UNIQUE, numero_serie VARCHAR(40) NOT NULL, "
                    + "serie_normalizada VARCHAR(80) NOT NULL UNIQUE, marca VARCHAR(150) NOT NULL, "
                    + "modelo VARCHAR(150) NOT NULL, categoria_id VARCHAR(36) NOT NULL, "
                    + "proveedor_id VARCHAR(36) NOT NULL, estado VARCHAR(30) NOT NULL, "
                    + "presente_en_almacen BOOLEAN NOT NULL, ubicacion_actual_id VARCHAR(36), "
                    + "ultima_ubicacion_almacen_id VARCHAR(36), destino_salida VARCHAR(500))",
            "CREATE TABLE IF NOT EXISTS configuracion_stock_minimo ("
                    + "id VARCHAR(36) PRIMARY KEY, categoria_id VARCHAR(36) NOT NULL, sede VARCHAR(150) NOT NULL, "
                    + "sede_normalizada VARCHAR(300) NOT NULL, minimo INTEGER NOT NULL CHECK (minimo >= 0), "
                    + "CONSTRAINT uq_stock_categoria_sede UNIQUE (categoria_id, sede_normalizada))",
            "CREATE TABLE IF NOT EXISTS movimiento ("
                    + "id VARCHAR(36) PRIMARY KEY, fecha_hora TIMESTAMP NOT NULL, tipo VARCHAR(30) NOT NULL, "
                    + "motivo VARCHAR(500) NOT NULL, equipo_id VARCHAR(36) NOT NULL, "
                    + "equipo_categoria_id VARCHAR(36) NOT NULL, equipo_codigo VARCHAR(80) NOT NULL, "
                    + "equipo_numero_serie VARCHAR(40) NOT NULL, equipo_marca VARCHAR(150) NOT NULL, "
                    + "equipo_modelo VARCHAR(150) NOT NULL, categoria_nombre VARCHAR(150) NOT NULL, "
                    + "proveedor_nombre VARCHAR(150) NOT NULL, responsable_id VARCHAR(36) NOT NULL, "
                    + "responsable_nombre VARCHAR(80) NOT NULL, origen_id VARCHAR(36), origen_sede VARCHAR(150), "
                    + "origen_ambiente VARCHAR(150), origen_area VARCHAR(150), origen_piso VARCHAR(40), "
                    + "origen_es_almacen BOOLEAN, destino_id VARCHAR(36), destino_sede VARCHAR(150), "
                    + "destino_ambiente VARCHAR(150), destino_area VARCHAR(150), destino_piso VARCHAR(40), "
                    + "destino_es_almacen BOOLEAN, destino_externo VARCHAR(500), estado_anterior VARCHAR(30), "
                    + "estado_nuevo VARCHAR(30), presencia_anterior BOOLEAN NOT NULL, "
                    + "presencia_nueva BOOLEAN NOT NULL)");

    private final String url;
    private final String usuario;
    private final String contrasena;
    private final Connection conexionAncla;

    private HsqlDatabase(String url, String usuario, String contrasena, Connection conexionAncla) {
        this.url = url;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.conexionAncla = conexionAncla;
    }

    public static HsqlDatabase abrir(String url, String usuario, String contrasena) throws SQLException {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(contrasena, "contrasena");
        prepararDirectorio(url);
        Connection conexion = DriverManager.getConnection(url, usuario, contrasena);
        HsqlDatabase base = new HsqlDatabase(url, usuario, contrasena, conexion);
        try {
            base.crearEsquema();
            return base;
        } catch (SQLException | RuntimeException e) {
            try {
                conexion.close();
            } catch (SQLException cierre) {
                e.addSuppressed(cierre);
            }
            throw e;
        }
    }

    public Connection nuevaConexion() throws SQLException {
        return DriverManager.getConnection(url, usuario, contrasena);
    }

    public <T> T ejecutar(OperacionSql<T> operacion) {
        try (Connection conexion = nuevaConexion()) {
            return operacion.ejecutar(conexion);
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    @Override
    public void close() throws SQLException {
        SQLException fallo = null;
        try (Statement sentencia = conexionAncla.createStatement()) {
            sentencia.execute("SHUTDOWN");
        } catch (SQLException e) {
            fallo = e;
        } finally {
            try {
                conexionAncla.close();
            } catch (SQLException e) {
                if (fallo == null) {
                    fallo = e;
                } else {
                    fallo.addSuppressed(e);
                }
            }
        }
        if (fallo != null) {
            throw fallo;
        }
    }

    static RuntimeException traducir(SQLException error) {
        if ("23505".equals(error.getSQLState())) {
            return new IllegalArgumentException("Ya existe un registro con esos datos únicos", error);
        }
        return new IllegalStateException("No se pudo acceder a la base HSQLDB", error);
    }

    private void crearEsquema() throws SQLException {
        try (Statement sentencia = conexionAncla.createStatement()) {
            for (String sql : ESQUEMA) {
                sentencia.execute(sql);
            }
        }
    }

    private static void prepararDirectorio(String url) throws SQLException {
        String prefijo = "jdbc:hsqldb:file:";
        if (!url.startsWith(prefijo)) {
            return;
        }
        String nombreBase = url.substring(prefijo.length()).split(";", 2)[0];
        Path directorio = Path.of(nombreBase).toAbsolutePath().getParent();
        if (directorio != null) {
            try {
                Files.createDirectories(directorio);
            } catch (IOException e) {
                throw new SQLException("No se pudo crear el directorio de la base HSQLDB", e);
            }
        }
    }

    @FunctionalInterface
    public interface OperacionSql<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }
}