package com.computototal.inventario.demo;

import com.computototal.inventario.dao.UsuarioDAO;
import com.computototal.inventario.dao.memoria.UsuarioDAOMemoria;
import com.computototal.inventario.modelo.Rol;
import com.computototal.inventario.modelo.Usuario;
import com.computototal.inventario.servicio.AutenticacionServicio;
import com.computototal.inventario.servicio.AutorizacionServicio;
import com.computototal.inventario.servicio.GestorContrasenas;
import com.computototal.inventario.servicio.Permiso;
import com.computototal.inventario.servicio.Sesion;
import com.computototal.inventario.servicio.SesionUsuario;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class VerificacionSeguridad {
    private static int verificaciones;

    private VerificacionSeguridad() {
    }

    public static void main(String[] args) {
        UsuarioDAO usuarioDAO = new UsuarioDAOMemoria();
        GestorContrasenas gestorContrasenas = new GestorContrasenas();
        Sesion sesion = new Sesion();
        AutenticacionServicio autenticacion = new AutenticacionServicio(usuarioDAO, gestorContrasenas, sesion);
        AutorizacionServicio autorizacion = new AutorizacionServicio(sesion);

        insertarUsuario(usuarioDAO, gestorContrasenas, id(1), "admin", Rol.ADMINISTRADOR, claveAdministrador());
        insertarUsuario(usuarioDAO, gestorContrasenas, id(2), "almacen", Rol.ALMACENERO, claveAlmacenero());
        insertarUsuario(usuarioDAO, gestorContrasenas, id(3), "auditor", Rol.AUDITOR, claveAuditor());

        System.out.println("Verificacion local de autenticacion, sesion y autorizacion");
        verificarInicioSesionPorRol(autenticacion, sesion);
        verificarCredencialesInvalidas(autenticacion, sesion);
        verificarCierreYReintentoFallido(autenticacion, autorizacion, sesion);
        verificarPermisosAdministrador(autenticacion, autorizacion);
        verificarPermisosAlmacenero(autenticacion, autorizacion);
        verificarPermisosAuditor(autenticacion, autorizacion, usuarioDAO, sesion);
        verificarHashYSal(gestorContrasenas);
        autenticacion.cerrarSesion();
        System.out.println("RESULTADO: " + verificaciones + " comprobaciones OK.");
    }

    private static void verificarInicioSesionPorRol(AutenticacionServicio autenticacion, Sesion sesion) {
        SesionUsuario administrador = autenticacion.iniciarSesion("ADMIN", claveAdministrador());
        comparar("Inicio de sesion de Administrador", Rol.ADMINISTRADOR, administrador.rol());
        autenticacion.cerrarSesion();

        SesionUsuario almacenero = autenticacion.iniciarSesion(" almacen ", claveAlmacenero());
        comparar("Inicio de sesion de Almacenero", Rol.ALMACENERO, almacenero.rol());
        autenticacion.cerrarSesion();

        SesionUsuario auditor = autenticacion.iniciarSesion("auditor", claveAuditor());
        comparar("Inicio de sesion de Auditor", Rol.AUDITOR, auditor.rol());
        comparar("Sesion expone solo la identidad autenticada", auditor, sesion.exigirSesionActiva());
        autenticacion.cerrarSesion();
    }

    private static void verificarCredencialesInvalidas(AutenticacionServicio autenticacion, Sesion sesion) {
        String mensajeContrasenaIncorrecta = mensajeDeRechazo(
                () -> autenticacion.iniciarSesion("admin", "no-es-la-clave".toCharArray()));
        String mensajeUsuarioInexistente = mensajeDeRechazo(
                () -> autenticacion.iniciarSesion("no-existe", "cualquier-clave".toCharArray()));

        comparar("Rechaza contrasena incorrecta", "Credenciales incorrectas", mensajeContrasenaIncorrecta);
        comparar("Rechaza usuario inexistente", "Credenciales incorrectas", mensajeUsuarioInexistente);
        comparar("Ambos rechazos usan el mismo mensaje generico",
                mensajeContrasenaIncorrecta, mensajeUsuarioInexistente);
        comparar("Intento invalido no autentica la sesion", false, sesion.estaActiva());
    }

    private static void verificarCierreYReintentoFallido(AutenticacionServicio autenticacion,
                                                          AutorizacionServicio autorizacion, Sesion sesion) {
        autenticacion.iniciarSesion("admin", claveAdministrador());
        comparar("Cerrar sesion elimina la identidad activa", false, cerrarYConsultar(autenticacion, sesion));
        comparar("Operaciones requieren sesion activa despues del cierre", true,
                rechazaPermisoSinSesion(autorizacion, Permiso.CONSULTAR_INVENTARIO));

        autenticacion.iniciarSesion("admin", claveAdministrador());
        comparar("Sesion valida se establece antes del intento fallido", true, sesion.estaActiva());
        String mensaje = mensajeDeRechazo(() -> autenticacion.iniciarSesion("admin", "clave-incorrecta".toCharArray()));
        comparar("El intento fallido conserva el mensaje generico", "Credenciales incorrectas", mensaje);
        comparar("Intento fallido posterior deja la sesion cerrada", false, sesion.estaActiva());
        comparar("Permisos tambien rechazan tras un intento fallido", true,
                rechazaPermisoSinSesion(autorizacion, Permiso.CONSULTAR_INVENTARIO));
    }

    private static void verificarPermisosAdministrador(AutenticacionServicio autenticacion,
                                                       AutorizacionServicio autorizacion) {
        autenticacion.iniciarSesion("admin", claveAdministrador());
        for (Permiso permiso : Permiso.values()) {
            exigirPermitido("Administrador: " + permiso, autorizacion, permiso);
        }
        autenticacion.cerrarSesion();
    }

    private static void verificarPermisosAlmacenero(AutenticacionServicio autenticacion,
                                                     AutorizacionServicio autorizacion) {
        autenticacion.iniciarSesion("almacen", claveAlmacenero());
        Set<Permiso> permitidos = EnumSet.of(
                Permiso.REGISTRAR_EQUIPOS,
                Permiso.ACTUALIZAR_EQUIPOS,
                Permiso.REGISTRAR_UBICACIONES,
                Permiso.ACTUALIZAR_UBICACIONES,
                Permiso.CAMBIAR_ESTADO_EQUIPO,
                Permiso.REGISTRAR_INGRESOS,
                Permiso.REGISTRAR_SALIDAS,
                Permiso.REGISTRAR_TRASLADOS,
                Permiso.CAMBIAR_UBICACION_EQUIPO,
                Permiso.CONFIGURAR_STOCK_MINIMO,
                Permiso.CONSULTAR_INVENTARIO,
                Permiso.CONSULTAR_ALERTAS,
                Permiso.CONSULTAR_HISTORIAL,
                Permiso.CONSULTAR_REPORTES);
        for (Permiso permiso : permitidos) {
            exigirPermitido("Almacenero: " + permiso, autorizacion, permiso);
        }
        exigirDenegado("Almacenero no administra usuarios", autorizacion, Permiso.ADMINISTRAR_USUARIOS);
        exigirDenegado("Almacenero no administra catalogos", autorizacion, Permiso.ADMINISTRAR_CATALOGOS);
        exigirDenegado("Almacenero no elimina registros", autorizacion, Permiso.ELIMINAR_REGISTROS);
        autenticacion.cerrarSesion();
    }

    private static void verificarPermisosAuditor(AutenticacionServicio autenticacion,
                                                  AutorizacionServicio autorizacion,
                                                  UsuarioDAO usuarioDAO, Sesion sesion) {
        SesionUsuario auditor = autenticacion.iniciarSesion("auditor", claveAuditor());
        Set<Permiso> consultas = EnumSet.of(
                Permiso.CONSULTAR_INVENTARIO,
                Permiso.CONSULTAR_ALERTAS,
                Permiso.CONSULTAR_HISTORIAL,
                Permiso.CONSULTAR_REPORTES);
        for (Permiso permiso : Permiso.values()) {
            if (consultas.contains(permiso)) {
                exigirPermitido("Auditor consulta: " + permiso, autorizacion, permiso);
            } else {
                exigirDenegado("Auditor no modifica: " + permiso, autorizacion, permiso);
            }
        }

        Usuario copiaMutable = usuarioDAO.buscarPorNombreUsuario("auditor").orElseThrow();
        copiaMutable.setRol(Rol.ADMINISTRADOR);
        comparar("Mutar copia de Usuario no cambia rol de la sesion", Rol.AUDITOR, sesion.exigirSesionActiva().rol());
        exigirDenegado("Mutar copia no eleva permisos", autorizacion, Permiso.ADMINISTRAR_USUARIOS);
        comparar("Sesion sigue asociada al mismo usuario", auditor.id(), sesion.exigirSesionActiva().id());
        autenticacion.cerrarSesion();
    }

    private static void verificarHashYSal(GestorContrasenas gestorContrasenas) {
        GestorContrasenas.Credencial primera = gestorContrasenas.generarHash("clave-compartida-demo".toCharArray());
        GestorContrasenas.Credencial segunda = gestorContrasenas.generarHash("clave-compartida-demo".toCharArray());
        boolean salesDistintas = !Arrays.equals(primera.getSalContrasena(), segunda.getSalContrasena());
        boolean primeraVerifica = gestorContrasenas.verificar("clave-compartida-demo".toCharArray(),
                primera.getHashContrasena(), primera.getSalContrasena());
        boolean segundaVerifica = gestorContrasenas.verificar("clave-compartida-demo".toCharArray(),
                segunda.getHashContrasena(), segunda.getSalContrasena());
        comparar("Hashes usan sales distintas para igual contrasena", true, salesDistintas);
        comparar("Primera credencial verifica", true, primeraVerifica);
        comparar("Segunda credencial verifica", true, segundaVerifica);
    }

    private static void insertarUsuario(UsuarioDAO dao, GestorContrasenas gestorContrasenas,
                                        UUID id, String nombre, Rol rol, char[] contrasena) {
        GestorContrasenas.Credencial credencial = gestorContrasenas.generarHash(contrasena);
        dao.insertar(new Usuario(id, nombre, credencial.getHashContrasena(), credencial.getSalContrasena(), rol));
    }

    private static boolean cerrarYConsultar(AutenticacionServicio autenticacion, Sesion sesion) {
        autenticacion.cerrarSesion();
        return sesion.estaActiva();
    }

    private static boolean rechazaPermisoSinSesion(AutorizacionServicio autorizacion, Permiso permiso) {
        try {
            autorizacion.exigirPermiso(permiso);
            return false;
        } catch (SecurityException excepcion) {
            return true;
        }
    }

    private static String mensajeDeRechazo(Runnable operacion) {
        try {
            operacion.run();
            return "SIN_RECHAZO";
        } catch (SecurityException excepcion) {
            return excepcion.getMessage();
        }
    }

    private static void exigirPermitido(String descripcion, AutorizacionServicio autorizacion, Permiso permiso) {
        boolean permitido = true;
        try {
            autorizacion.exigirPermiso(permiso);
        } catch (SecurityException excepcion) {
            permitido = false;
        }
        comparar(descripcion, true, permitido);
    }

    private static void exigirDenegado(String descripcion, AutorizacionServicio autorizacion, Permiso permiso) {
        boolean denegado = false;
        try {
            autorizacion.exigirPermiso(permiso);
        } catch (SecurityException excepcion) {
            denegado = true;
        }
        comparar(descripcion, true, denegado);
    }

    private static char[] claveAdministrador() {
        return "demo-admin-2026".toCharArray();
    }

    private static char[] claveAlmacenero() {
        return "demo-almacen-2026".toCharArray();
    }

    private static char[] claveAuditor() {
        return "demo-auditor-2026".toCharArray();
    }

    private static UUID id(long numero) {
        return new UUID(0L, numero);
    }

    private static void comparar(String descripcion, Object esperado, Object obtenido) {
        boolean correcto = Objects.equals(esperado, obtenido);
        System.out.println((correcto ? "OK" : "FALLO") + " | " + descripcion
                + " | esperado=" + esperado + " | obtenido=" + obtenido);
        if (!correcto) {
            throw new IllegalStateException("Verificacion fallida: " + descripcion);
        }
        verificaciones++;
    }
}