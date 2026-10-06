package com.computototal.inventario.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.computototal.inventario.dao.ProveedorDAO;
import com.computototal.inventario.modelo.Proveedor;

public final class ProveedorDAOJdbc extends JdbcDAO implements ProveedorDAO {
    public ProveedorDAOJdbc(HsqlDatabase base) {
        super(base);
    }

    @Override
    public void insertar(Proveedor proveedor) {
        Proveedor valor = Objects.requireNonNull(proveedor, "proveedor");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO proveedor (id, nombre, telefono, correo_electronico) VALUES (?, ?, ?, ?)")) {
                ponerId(sentencia, 1, valor.getId());
                sentencia.setString(2, valor.getNombre());
                sentencia.setString(3, valor.getTelefono());
                sentencia.setString(4, valor.getCorreoElectronico());
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<Proveedor> buscarPorId(UUID id) {
        Objects.requireNonNull(id, "id");
        return base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, nombre, telefono, correo_electronico FROM proveedor WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                try (ResultSet fila = sentencia.executeQuery()) {
                    return fila.next() ? Optional.of(mapear(fila)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Proveedor> listar() {
        return base.ejecutar(conexion -> {
            List<Proveedor> resultado = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, nombre, telefono, correo_electronico FROM proveedor ORDER BY id");
                    ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    resultado.add(mapear(filas));
                }
            }
            return List.copyOf(resultado);
        });
    }

    @Override
    public void actualizar(Proveedor proveedor) {
        Proveedor valor = Objects.requireNonNull(proveedor, "proveedor");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE proveedor SET nombre = ?, telefono = ?, correo_electronico = ? WHERE id = ?")) {
                sentencia.setString(1, valor.getNombre());
                sentencia.setString(2, valor.getTelefono());
                sentencia.setString(3, valor.getCorreoElectronico());
                ponerId(sentencia, 4, valor.getId());
                exigirActualizado(sentencia.executeUpdate(), "proveedor", valor.getId());
            }
            return null;
        });
    }

    @Override
    public void eliminar(UUID id) {
        Objects.requireNonNull(id, "id");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement("DELETE FROM proveedor WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                exigirActualizado(sentencia.executeUpdate(), "proveedor", id);
            }
            return null;
        });
    }

    private static Proveedor mapear(ResultSet fila) throws SQLException {
        return new Proveedor(leerId(fila, "id"), leerTexto(fila, "nombre"), leerTexto(fila, "telefono"),
                leerTexto(fila, "correo_electronico"));
    }
}