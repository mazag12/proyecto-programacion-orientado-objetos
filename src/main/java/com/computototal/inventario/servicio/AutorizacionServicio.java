package com.computototal.inventario.servicio;

import com.computototal.inventario.modelo.Rol;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AutorizacionServicio {
    private final Sesion sesion;
    private final Map<Rol, Set<Permiso>> permisosPorRol;

    public AutorizacionServicio(Sesion sesion) {
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.permisosPorRol = crearPermisosPorRol();
    }

    public boolean tienePermiso(Permiso permiso) {
        SesionUsuario usuario = sesion.exigirSesionActiva();
        Permiso permisoValidado = Objects.requireNonNull(permiso, "permiso");
        return permisosPorRol.get(usuario.rol()).contains(permisoValidado);
    }

    public void exigirPermiso(Permiso permiso) {
        SesionUsuario usuario = sesion.exigirSesionActiva();
        Permiso permisoValidado = Objects.requireNonNull(permiso, "permiso");
        if (!permisosPorRol.get(usuario.rol()).contains(permisoValidado)) {
            throw new SecurityException("No tiene permiso para realizar esta operacion");
        }
    }

    private Map<Rol, Set<Permiso>> crearPermisosPorRol() {
        Map<Rol, Set<Permiso>> permisos = new EnumMap<>(Rol.class);
        permisos.put(Rol.ADMINISTRADOR, Collections.unmodifiableSet(EnumSet.allOf(Permiso.class)));
        permisos.put(Rol.ALMACENERO, conjunto(
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
                Permiso.CONSULTAR_REPORTES));
        permisos.put(Rol.AUDITOR, conjunto(
                Permiso.CONSULTAR_INVENTARIO,
                Permiso.CONSULTAR_ALERTAS,
                Permiso.CONSULTAR_HISTORIAL,
                Permiso.CONSULTAR_REPORTES));
        return Collections.unmodifiableMap(permisos);
    }

    private Set<Permiso> conjunto(Permiso... permisos) {
        EnumSet<Permiso> resultado = EnumSet.noneOf(Permiso.class);
        Collections.addAll(resultado, permisos);
        return Collections.unmodifiableSet(resultado);
    }
}