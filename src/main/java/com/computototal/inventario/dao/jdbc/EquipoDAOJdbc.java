package com.computototal.inventario.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.computototal.inventario.dao.EquipoDAO;
import com.computototal.inventario.modelo.Equipo;
import com.computototal.inventario.modelo.EstadoEquipo;

public final class EquipoDAOJdbc extends JdbcDAO implements EquipoDAO {
    private static final String COLUMNAS = "id, codigo, numero_serie, marca, modelo, categoria_id, proveedor_id, "
            + "estado, presente_en_almacen, ubicacion_actual_id, ultima_ubicacion_almacen_id, destino_salida";

    public EquipoDAOJdbc(HsqlDatabase base) {
        super(base);
    }

    @Override
    public void insertar(Equipo equipo) {
        Equipo valor = Objects.requireNonNull(equipo, "equipo");
        base.ejecutar(conexion -> {
            String sql = "INSERT INTO equipo (id, codigo, codigo_normalizado, numero_serie, serie_normalizada, "
                    + "marca, modelo, categoria_id, proveedor_id, estado, presente_en_almacen, "
                    + "ubicacion_actual_id, ultima_ubicacion_almacen_id, destino_salida) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                ponerId(sentencia, 1, valor.getId());
                sentencia.setString(2, valor.getCodigo());
                sentencia.setString(3, clave(valor.getCodigo(), "codigo"));
                sentencia.setString(4, valor.getNumeroSerie());
                sentencia.setString(5, clave(valor.getNumeroSerie(), "numeroSerie"));
                sentencia.setString(6, valor.getMarca());
                sentencia.setString(7, valor.getModelo());
                ponerId(sentencia, 8, valor.getCategoriaId());
                ponerId(sentencia, 9, valor.getProveedorId());
                sentencia.setString(10, valor.getEstado().name());
                sentencia.setBoolean(11, valor.isPresenteEnAlmacen());
                ponerIdOpcional(sentencia, 12, valor.getUbicacionActualId());
                ponerIdOpcional(sentencia, 13, valor.getUltimaUbicacionAlmacenId());
                ponerTextoOpcional(sentencia, 14, valor.getDestinoSalida());
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<Equipo> buscarPorId(UUID id) {
        Objects.requireNonNull(id, "id");
        return buscar("id", id.toString());
    }

    @Override
    public Optional<Equipo> buscarPorCodigo(String codigo) {
        return buscar("codigo_normalizado", clave(codigo, "codigo"));
    }

    @Override
    public Optional<Equipo> buscarPorNumeroSerie(String numeroSerie) {
        return buscar("serie_normalizada", clave(numeroSerie, "numeroSerie"));
    }

    private Optional<Equipo> buscar(String columna, String valor) {
        return base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM equipo WHERE " + columna + " = ?")) {
                sentencia.setString(1, valor);
                try (ResultSet fila = sentencia.executeQuery()) {
                    return fila.next() ? Optional.of(mapear(fila)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Equipo> listar() {
        return base.ejecutar(conexion -> {
            List<Equipo> resultado = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM equipo ORDER BY id");
                    ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    resultado.add(mapear(filas));
                }
            }
            return List.copyOf(resultado);
        });
    }

    @Override
    public void actualizar(Equipo equipo) {
        Equipo valor = Objects.requireNonNull(equipo, "equipo");
        base.ejecutar(conexion -> {
            String sql = "UPDATE equipo SET codigo = ?, codigo_normalizado = ?, numero_serie = ?, "
                    + "serie_normalizada = ?, marca = ?, modelo = ?, categoria_id = ?, proveedor_id = ?, "
                    + "estado = ?, presente_en_almacen = ?, ubicacion_actual_id = ?, "
                    + "ultima_ubicacion_almacen_id = ?, destino_salida = ? WHERE id = ?";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, valor.getCodigo());
                sentencia.setString(2, clave(valor.getCodigo(), "codigo"));
                sentencia.setString(3, valor.getNumeroSerie());
                sentencia.setString(4, clave(valor.getNumeroSerie(), "numeroSerie"));
                sentencia.setString(5, valor.getMarca());
                sentencia.setString(6, valor.getModelo());
                ponerId(sentencia, 7, valor.getCategoriaId());
                ponerId(sentencia, 8, valor.getProveedorId());
                sentencia.setString(9, valor.getEstado().name());
                sentencia.setBoolean(10, valor.isPresenteEnAlmacen());
                ponerIdOpcional(sentencia, 11, valor.getUbicacionActualId());
                ponerIdOpcional(sentencia, 12, valor.getUltimaUbicacionAlmacenId());
                ponerTextoOpcional(sentencia, 13, valor.getDestinoSalida());
                ponerId(sentencia, 14, valor.getId());
                exigirActualizado(sentencia.executeUpdate(), "equipo", valor.getId());
            }
            return null;
        });
    }

    @Override
    public void eliminar(UUID id) {
        Objects.requireNonNull(id, "id");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement("DELETE FROM equipo WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                exigirActualizado(sentencia.executeUpdate(), "equipo", id);
            }
            return null;
        });
    }

    private static Equipo mapear(ResultSet fila) throws SQLException {
        return Equipo.restaurar(leerId(fila, "id"), leerTexto(fila, "codigo"),
                leerTexto(fila, "numero_serie"), leerTexto(fila, "marca"), leerTexto(fila, "modelo"),
                leerId(fila, "categoria_id"), leerId(fila, "proveedor_id"),
                EstadoEquipo.valueOf(leerTexto(fila, "estado")), fila.getBoolean("presente_en_almacen"),
                leerIdOpcional(fila, "ubicacion_actual_id"), leerIdOpcional(fila, "ultima_ubicacion_almacen_id"),
                leerTextoOpcional(fila, "destino_salida"));
    }
}