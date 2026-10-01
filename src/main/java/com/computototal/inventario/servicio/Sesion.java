package com.computototal.inventario.servicio;

import com.computototal.inventario.modelo.Usuario;
import java.util.Optional;
import java.util.Objects;

public final class Sesion {
    private SesionUsuario usuarioAutenticado;

    public void cerrarSesion() {
        usuarioAutenticado = null;
    }

    public Optional<SesionUsuario> obtenerUsuarioAutenticado() {
        return Optional.ofNullable(usuarioAutenticado);
    }

    public SesionUsuario exigirSesionActiva() {
        if (usuarioAutenticado == null) {
            throw new SecurityException("Se requiere una sesion activa");
        }
        return usuarioAutenticado;
    }

    public boolean estaActiva() {
        return usuarioAutenticado != null;
    }

    void registrarAutenticacion(Usuario usuario) {
        Usuario validado = Objects.requireNonNull(usuario, "usuario");
        usuarioAutenticado = new SesionUsuario(validado.getId(), validado.getNombreUsuario(), validado.getRol());
    }
}