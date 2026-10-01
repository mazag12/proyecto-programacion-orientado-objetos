package com.computototal.inventario.servicio;

import com.computototal.inventario.dao.UsuarioDAO;
import com.computototal.inventario.modelo.Usuario;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class AutenticacionServicio {
    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Credenciales incorrectas";

    private final UsuarioDAO usuarioDAO;
    private final GestorContrasenas gestorContrasenas;
    private final Sesion sesion;

    public AutenticacionServicio(UsuarioDAO usuarioDAO, GestorContrasenas gestorContrasenas, Sesion sesion) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO");
        this.gestorContrasenas = Objects.requireNonNull(gestorContrasenas, "gestorContrasenas");
        this.sesion = Objects.requireNonNull(sesion, "sesion");
    }

    public SesionUsuario iniciarSesion(String nombreUsuario, char[] contrasena) {
        sesion.cerrarSesion();
        byte[] hash = null;
        byte[] sal = null;
        try {
            if (nombreUsuario == null || nombreUsuario.strip().isEmpty() || contrasena == null) {
                throw credencialesInvalidas();
            }
            Optional<Usuario> usuarioEncontrado;
            try {
                usuarioEncontrado = usuarioDAO.buscarPorNombreUsuario(nombreUsuario);
            } catch (IllegalArgumentException excepcion) {
                throw credencialesInvalidas();
            }
            if (usuarioEncontrado.isEmpty()) {
                throw credencialesInvalidas();
            }

            Usuario usuario = usuarioEncontrado.get();
            hash = usuario.getHashContrasena();
            sal = usuario.getSalContrasena();
            if (!gestorContrasenas.verificar(contrasena, hash, sal)) {
                throw credencialesInvalidas();
            }

            sesion.registrarAutenticacion(usuario);
            return sesion.exigirSesionActiva();
        } finally {
            if (contrasena != null) {
                Arrays.fill(contrasena, '\0');
            }
            if (hash != null) {
                Arrays.fill(hash, (byte) 0);
            }
            if (sal != null) {
                Arrays.fill(sal, (byte) 0);
            }
        }
    }

    public void cerrarSesion() {
        sesion.cerrarSesion();
    }

    public Optional<SesionUsuario> obtenerUsuarioAutenticado() {
        return sesion.obtenerUsuarioAutenticado();
    }

    public SesionUsuario exigirSesionActiva() {
        return sesion.exigirSesionActiva();
    }

    private SecurityException credencialesInvalidas() {
        return new SecurityException(MENSAJE_CREDENCIALES_INVALIDAS);
    }
}