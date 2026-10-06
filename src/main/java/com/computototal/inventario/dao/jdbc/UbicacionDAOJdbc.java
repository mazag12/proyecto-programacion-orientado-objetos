package com.computototal.inventario.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.computototal.inventario.dao.UbicacionDAO;
import com.computototal.inventario.modelo.Ubicacion;

public final class UbicacionDAOJdbc extends JdbcDAO implements UbicacionDAO {
    public UbicacionDAOJdbc(HsqlDatabase base) {
        super(base);
    }

    @Override
    public void insertar(Ubicacion ubicacion) {
        Ubicacion valor = Objects.requireNonNull(ubicacion, "ubicacion");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO ubicacion (id, sede, ambiente, area, piso, es_almacen) VALUES (?, ?, ?, ?, ?, ?)")) {
                ponerId(sentencia, 1, valor.getId());
                sentencia.setString(2, valor.getSede());
                sentencia.setString(3, valor.getAmbiente());
                sentencia.setString(4, valor.getArea());
                sentencia.setString(5, valor.getPiso());
                sentencia.setBoolean(6, valor.isAlmacen());
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<Ubicacion> buscarPorId(UUID id) {
        Objects.requireNonNull(id, "id");
        return base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, sede, ambiente, area, piso, es_almacen FROM ubicacion WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                try (ResultSet fila = sentencia.executeQuery()) {
                    return fila.next() ? Optional.of(mapear(fila)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Ubicacion> listar() {
        return base.ejecutar(conexion -> {
            List<Ubicacion> resultado = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, sede, ambiente, area, piso, es_almacen FROM ubicacion ORDER BY id");
                    ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    resultado.add(mapear(filas));
                }
            }
            return List.copyOf(resultado);
        });
    }

    @Override
    public void actualizar(Ubicacion ubicacion) {
        Ubicacion valor = Objects.requireNonNull(ubicacion, "ubicacion");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE ubicacion SET sede = ?, ambiente = ?, area = ?, piso = ?, es_almacen = ? WHERE id = ?")) {
                sentencia.setString(1, valor.getSede());
                sentencia.setString(2, valor.getAmbiente());
                sentencia.setString(3, valor.getArea());
                sentencia.setString(4, valor.getPiso());
                sentencia.setBoolean(5, valor.isAlmacen());
                ponerId(sentencia, 6, valor.getId());
                exigirActualizado(sentencia.executeUpdate(), "ubicacion", valor.getId());
            }
            return null;
        });
    }

    @Override
    public void eliminar(UUID id) {
        Objects.requireNonNull(id, "id");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement("DELETE FROM ubicacion WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                exigirActualizado(sentencia.executeUpdate(), "ubicacion", id);
            }
            return null;
        });
    }

    private static Ubicacion mapear(ResultSet fila) throws SQLException {
        return new Ubicacion(leerId(fila, "id"), leerTexto(fila, "sede"), leerTexto(fila, "ambiente"),
                leerTexto(fila, "area"), leerTexto(fila, "piso"), fila.getBoolean("es_almacen"));
    }
}