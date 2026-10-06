package com.computototal.inventario.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.computototal.inventario.dao.UsuarioDAO;
import com.computototal.inventario.modelo.Rol;
import com.computototal.inventario.modelo.Usuario;

public final class UsuarioDAOJdbc extends JdbcDAO implements UsuarioDAO {
    public UsuarioDAOJdbc(HsqlDatabase base) {
        super(base);
    }

    @Override
    public void insertar(Usuario usuario) {
        Usuario valor = Objects.requireNonNull(usuario, "usuario");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO usuario (id, nombre_usuario, nombre_usuario_normalizado, hash_contrasena, "
                            + "sal_contrasena, rol) VALUES (?, ?, ?, ?, ?, ?)")) {
                ponerId(sentencia, 1, valor.getId());
                sentencia.setString(2, valor.getNombreUsuario());
                sentencia.setString(3, clave(valor.getNombreUsuario(), "nombreUsuario"));
                sentencia.setBytes(4, valor.getHashContrasena());
                sentencia.setBytes(5, valor.getSalContrasena());
                sentencia.setString(6, valor.getRol().name());
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        Objects.requireNonNull(id, "id");
        return base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, nombre_usuario, hash_contrasena, sal_contrasena, rol FROM usuario WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                try (ResultSet fila = sentencia.executeQuery()) {
                    return fila.next() ? Optional.of(mapear(fila)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        String nombreNormalizado = clave(nombreUsuario, "nombreUsuario");
        return base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, nombre_usuario, hash_contrasena, sal_contrasena, rol "
                            + "FROM usuario WHERE nombre_usuario_normalizado = ?")) {
                sentencia.setString(1, nombreNormalizado);
                try (ResultSet fila = sentencia.executeQuery()) {
                    return fila.next() ? Optional.of(mapear(fila)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Usuario> listar() {
        return base.ejecutar(conexion -> {
            List<Usuario> resultado = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, nombre_usuario, hash_contrasena, sal_contrasena, rol FROM usuario ORDER BY id");
                    ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    resultado.add(mapear(filas));
                }
            }
            return List.copyOf(resultado);
        });
    }

    @Override
    public void actualizar(Usuario usuario) {
        Usuario valor = Objects.requireNonNull(usuario, "usuario");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE usuario SET nombre_usuario = ?, nombre_usuario_normalizado = ?, hash_contrasena = ?, "
                            + "sal_contrasena = ?, rol = ? WHERE id = ?")) {
                sentencia.setString(1, valor.getNombreUsuario());
                sentencia.setString(2, clave(valor.getNombreUsuario(), "nombreUsuario"));
                sentencia.setBytes(3, valor.getHashContrasena());
                sentencia.setBytes(4, valor.getSalContrasena());
                sentencia.setString(5, valor.getRol().name());
                ponerId(sentencia, 6, valor.getId());
                exigirActualizado(sentencia.executeUpdate(), "usuario", valor.getId());
            }
            return null;
        });
    }

    @Override
    public void eliminar(UUID id) {
        Objects.requireNonNull(id, "id");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                exigirActualizado(sentencia.executeUpdate(), "usuario", id);
            }
            return null;
        });
    }

    private static Usuario mapear(ResultSet fila) throws SQLException {
        return new Usuario(leerId(fila, "id"), leerTexto(fila, "nombre_usuario"), fila.getBytes("hash_contrasena"),
                fila.getBytes("sal_contrasena"), Rol.valueOf(leerTexto(fila, "rol")));
    }
}