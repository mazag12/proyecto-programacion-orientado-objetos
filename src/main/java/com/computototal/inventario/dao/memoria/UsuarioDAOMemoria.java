package com.computototal.inventario.dao.memoria;

import com.computototal.inventario.dao.UsuarioDAO;
import com.computototal.inventario.modelo.Usuario;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UsuarioDAOMemoria implements UsuarioDAO {
    private final Map<UUID, Usuario> usuarios = new LinkedHashMap<>();

    @Override
    public void insertar(Usuario usuario) {
        Usuario validado = ValidacionDAO.entidad(usuario, "usuario");
        UUID id = ValidacionDAO.id(validado.getId());
        if (usuarios.containsKey(id)) {
            throw ValidacionDAO.duplicado("identificador de usuario", id.toString());
        }
        validarNombreUsuario(validado, null);
        Usuario copia = CopiasModelo.copiar(validado);
        usuarios.put(id, copia);
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return Optional.ofNullable(usuarios.get(ValidacionDAO.id(id))).map(CopiasModelo::copiar);
    }

    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        String clave = ValidacionDAO.textoClave(nombreUsuario, "nombreUsuario");
        return usuarios.values().stream()
                .filter(usuario -> ValidacionDAO.textoClave(usuario.getNombreUsuario(), "nombreUsuario").equals(clave))
                .findFirst()
                .map(CopiasModelo::copiar);
    }

    @Override
    public List<Usuario> listar() {
        List<Usuario> resultado = new ArrayList<>(usuarios.size());
        usuarios.values().forEach(usuario -> resultado.add(CopiasModelo.copiar(usuario)));
        return List.copyOf(resultado);
    }

    @Override
    public void actualizar(Usuario usuario) {
        Usuario validado = ValidacionDAO.entidad(usuario, "usuario");
        UUID id = ValidacionDAO.id(validado.getId());
        if (!usuarios.containsKey(id)) {
            throw ValidacionDAO.inexistente("usuario", id);
        }
        validarNombreUsuario(validado, id);
        Usuario copia = CopiasModelo.copiar(validado);
        usuarios.put(id, copia);
    }

    @Override
    public void eliminar(UUID id) {
        UUID idValidado = ValidacionDAO.id(id);
        if (usuarios.remove(idValidado) == null) {
            throw ValidacionDAO.inexistente("usuario", idValidado);
        }
    }

    private void validarNombreUsuario(Usuario candidato, UUID idExcluido) {
        String nombre = ValidacionDAO.textoClave(candidato.getNombreUsuario(), "nombreUsuario");
        for (Usuario existente : usuarios.values()) {
            if (!existente.getId().equals(idExcluido)
                    && ValidacionDAO.textoClave(existente.getNombreUsuario(), "nombreUsuario").equals(nombre)) {
                throw ValidacionDAO.duplicado("nombre de usuario", candidato.getNombreUsuario());
            }
        }
    }
}