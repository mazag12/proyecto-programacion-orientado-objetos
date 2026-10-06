package com.computototal.inventario.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.computototal.inventario.dao.ConfiguracionStockMinimoDAO;
import com.computototal.inventario.modelo.ConfiguracionStockMinimo;

public final class ConfiguracionStockMinimoDAOJdbc extends JdbcDAO implements ConfiguracionStockMinimoDAO {
    public ConfiguracionStockMinimoDAOJdbc(HsqlDatabase base) {
        super(base);
    }

    @Override
    public void guardar(ConfiguracionStockMinimo configuracion) {
        ConfiguracionStockMinimo valor = Objects.requireNonNull(configuracion, "configuracion");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO configuracion_stock_minimo "
                            + "(id, categoria_id, sede, sede_normalizada, minimo) VALUES (?, ?, ?, ?, ?)")) {
                ponerId(sentencia, 1, valor.getId());
                ponerId(sentencia, 2, valor.getCategoriaId());
                sentencia.setString(3, valor.getSede());
                sentencia.setString(4, clave(valor.getSede(), "sede"));
                sentencia.setInt(5, valor.getMinimo());
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<ConfiguracionStockMinimo> buscarPorId(UUID id) {
        Objects.requireNonNull(id, "id");
        return base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, categoria_id, sede, minimo FROM configuracion_stock_minimo WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                try (ResultSet fila = sentencia.executeQuery()) {
                    return fila.next() ? Optional.of(mapear(fila)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<ConfiguracionStockMinimo> buscarPorCategoriaYSede(UUID categoriaId, String sede) {
        Objects.requireNonNull(categoriaId, "categoriaId");
        String sedeNormalizada = clave(sede, "sede");
        return base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, categoria_id, sede, minimo FROM configuracion_stock_minimo "
                            + "WHERE categoria_id = ? AND sede_normalizada = ?")) {
                ponerId(sentencia, 1, categoriaId);
                sentencia.setString(2, sedeNormalizada);
                try (ResultSet fila = sentencia.executeQuery()) {
                    return fila.next() ? Optional.of(mapear(fila)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<ConfiguracionStockMinimo> listar() {
        return base.ejecutar(conexion -> {
            List<ConfiguracionStockMinimo> resultado = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, categoria_id, sede, minimo FROM configuracion_stock_minimo ORDER BY id");
                    ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    resultado.add(mapear(filas));
                }
            }
            return List.copyOf(resultado);
        });
    }

    @Override
    public void actualizar(ConfiguracionStockMinimo configuracion) {
        ConfiguracionStockMinimo valor = Objects.requireNonNull(configuracion, "configuracion");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE configuracion_stock_minimo SET categoria_id = ?, sede = ?, sede_normalizada = ?, "
                            + "minimo = ? WHERE id = ?")) {
                ponerId(sentencia, 1, valor.getCategoriaId());
                sentencia.setString(2, valor.getSede());
                sentencia.setString(3, clave(valor.getSede(), "sede"));
                sentencia.setInt(4, valor.getMinimo());
                ponerId(sentencia, 5, valor.getId());
                exigirActualizado(sentencia.executeUpdate(), "configuracion de stock minimo", valor.getId());
            }
            return null;
        });
    }

    @Override
    public void eliminar(UUID id) {
        Objects.requireNonNull(id, "id");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "DELETE FROM configuracion_stock_minimo WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                exigirActualizado(sentencia.executeUpdate(), "configuracion de stock minimo", id);
            }
            return null;
        });
    }

    private static ConfiguracionStockMinimo mapear(ResultSet fila) throws SQLException {
        return new ConfiguracionStockMinimo(leerId(fila, "id"), leerId(fila, "categoria_id"),
                leerTexto(fila, "sede"), fila.getInt("minimo"));
    }
}