package com.computototal.inventario.dao;

import com.computototal.inventario.modelo.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioDAO {
    void insertar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario);

    List<Usuario> listar();

    void actualizar(Usuario usuario);

    void eliminar(UUID id);
}