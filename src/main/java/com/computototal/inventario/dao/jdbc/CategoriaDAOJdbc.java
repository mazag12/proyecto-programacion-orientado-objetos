package com.computototal.inventario.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.computototal.inventario.dao.CategoriaDAO;
import com.computototal.inventario.modelo.Categoria;

public final class CategoriaDAOJdbc extends JdbcDAO implements CategoriaDAO {
    public CategoriaDAOJdbc(HsqlDatabase base) {
        super(base);
    }

    @Override
    public void insertar(Categoria categoria) {
        Categoria valor = Objects.requireNonNull(categoria, "categoria");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO categoria (id, nombre, descripcion) VALUES (?, ?, ?)")) {
                ponerId(sentencia, 1, valor.getId());
                sentencia.setString(2, valor.getNombre());
                sentencia.setString(3, valor.getDescripcion());
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<Categoria> buscarPorId(UUID id) {
        Objects.requireNonNull(id, "id");
        return base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, nombre, descripcion FROM categoria WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                try (ResultSet fila = sentencia.executeQuery()) {
                    return fila.next() ? Optional.of(mapear(fila)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Categoria> listar() {
        return base.ejecutar(conexion -> {
            List<Categoria> resultado = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, nombre, descripcion FROM categoria ORDER BY id");
                    ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    resultado.add(mapear(filas));
                }
            }
            return List.copyOf(resultado);
        });
    }

    @Override
    public void actualizar(Categoria categoria) {
        Categoria valor = Objects.requireNonNull(categoria, "categoria");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE categoria SET nombre = ?, descripcion = ? WHERE id = ?")) {
                sentencia.setString(1, valor.getNombre());
                sentencia.setString(2, valor.getDescripcion());
                ponerId(sentencia, 3, valor.getId());
                exigirActualizado(sentencia.executeUpdate(), "categoria", valor.getId());
            }
            return null;
        });
    }

    @Override
    public void eliminar(UUID id) {
        eliminarPorId("categoria", id);
    }

    private void eliminarPorId(String tabla, UUID id) {
        Objects.requireNonNull(id, "id");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement("DELETE FROM " + tabla + " WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                exigirActualizado(sentencia.executeUpdate(), "categoria", id);
            }
            return null;
        });
    }

    private static Categoria mapear(ResultSet fila) throws SQLException {
        return new Categoria(leerId(fila, "id"), leerTexto(fila, "nombre"), leerTexto(fila, "descripcion"));
    }
}