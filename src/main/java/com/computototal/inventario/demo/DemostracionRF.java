package com.computototal.inventario.demo;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.computototal.inventario.datos.DatosIniciales;
import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.TipoMovimiento;
import com.computototal.inventario.servicio.AutenticacionServicio;
import com.computototal.inventario.servicio.AutorizacionServicio;
import com.computototal.inventario.servicio.EquipoConsulta;
import com.computototal.inventario.servicio.EquipoServicio;
import com.computototal.inventario.servicio.MovimientoServicio;
import com.computototal.inventario.servicio.Permiso;
import com.computototal.inventario.servicio.ReporteServicio;
import com.computototal.inventario.servicio.StockServicio;

public final class DemostracionRF {
    private int demostraciones;
    private final AutenticacionServicio autenticacion;
    private final AutorizacionServicio autorizacion;
    private final EquipoServicio equipos;
    private final MovimientoServicio movimientos;
    private final StockServicio stock;
    private final ReporteServicio reportes;

    public DemostracionRF(AutenticacionServicio autenticacion, AutorizacionServicio autorizacion,
                          EquipoServicio equipos, MovimientoServicio movimientos,
                          StockServicio stock, ReporteServicio reportes) {
        this.autenticacion = Objects.requireNonNull(autenticacion, "autenticacion");
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
        this.equipos = Objects.requireNonNull(equipos, "equipos");
        this.movimientos = Objects.requireNonNull(movimientos, "movimientos");
        this.stock = Objects.requireNonNull(stock, "stock");
        this.reportes = Objects.requireNonNull(reportes, "reportes");
    }

    public void ejecutar() {
        demostrarAutenticacion();
        try {
            demostrarRegistro();
            demostrarUbicacionYMovimientos();
            demostrarEstadoEHistorial();
            demostrarStockYAlertas();
            demostrarReporte();
            demostrarDuplicado();
            demostrarRoles();
        } finally {
            autenticacion.cerrarSesion();
        }
        System.out.println("DEMO COMPLETADA: " + demostraciones + " requisitos verificados.");
    }

        private void demostrarAutenticacion() {
        autenticar(DatosIniciales.USUARIO_ADMIN, DatosIniciales.CLAVE_ADMIN);
        verificar("RF-011 inicio de sesion valido", true, autenticacion.obtenerUsuarioAutenticado().isPresent());
        autenticacion.cerrarSesion();
        String mensaje = mensajeRechazo(() -> autenticacion.iniciarSesion(
            DatosIniciales.USUARIO_ADMIN, "clave-incorrecta".toCharArray()));
        verificar("RF-011 credencial incorrecta usa mensaje generico", "Credenciales incorrectas", mensaje);
        verificar("RF-011 fallo de login no deja sesion activa", false,
            autenticacion.obtenerUsuarioAutenticado().isPresent());
        autenticar(DatosIniciales.USUARIO_ADMIN, DatosIniciales.CLAVE_ADMIN);
        }

    private void demostrarRegistro() {
        EquipoConsulta equipo = equipos.registrarEquipoConIngreso(
                id(901), "DEMO-901", "DEMO-SERIE-901", "Lenovo", "ThinkPad Demo",
                id(1), id(11), id(21), "Alta de demostracion").equipo();
        verificar("RF-01 registro de equipo con ingreso", true,
                equipo.presenteEnAlmacen() && equipo.estado() == EstadoEquipo.DISPONIBLE);
    }

    private void demostrarUbicacionYMovimientos() {
        TipoMovimiento cambio = movimientos.cambiarUbicacion(id(901), id(22), "Reubicacion demo").movimiento().getTipo();
        verificar("RF-02 cambio de ubicacion", TipoMovimiento.CAMBIO_UBICACION, cambio);
        TipoMovimiento traslado = movimientos.trasladar(id(901), id(24), "Traslado demo").movimiento().getTipo();
        verificar("RF-03 traslado entre sedes", TipoMovimiento.TRASLADO, traslado);
        TipoMovimiento salida = movimientos.registrarSalida(id(901), "Oficina demo", "Asignacion demo")
                .movimiento().getTipo();
        verificar("RF-03 salida con destino", TipoMovimiento.SALIDA, salida);
        TipoMovimiento ingreso = movimientos.registrarIngreso(id(901), id(24), "Devolucion demo")
                .movimiento().getTipo();
        verificar("RF-03 reingreso", TipoMovimiento.INGRESO, ingreso);
    }

    private void demostrarEstadoEHistorial() {
        movimientos.cambiarEstado(id(901), EstadoEquipo.EN_MANTENIMIENTO, "Revision demo");
        verificar("RF-06 cambio de estado", EstadoEquipo.EN_MANTENIMIENTO,
                equipos.consultarPorId(id(901)).estado());
        verificar("RF-07 consulta por codigo", id(901), equipos.consultarPorCodigo("DEMO-901").id());
        verificar("RF-07 consulta por serie", id(901), equipos.consultarPorNumeroSerie("DEMO-SERIE-901").id());
        verificar("RF-08 historial con responsable", true,
                movimientos.historialPorEquipo(id(901)).stream()
                        .allMatch(movimiento -> !movimiento.getNombreResponsable().isBlank()));
        verificar("RF-08 historial conserva salida y reingreso", true,
                movimientos.historialPorEquipo(id(901)).stream()
                        .anyMatch(movimiento -> movimiento.getTipo() == TipoMovimiento.SALIDA)
                        && movimientos.historialPorEquipo(id(901)).stream()
                        .anyMatch(movimiento -> movimiento.getTipo() == TipoMovimiento.INGRESO));
    }

    private void demostrarStockYAlertas() {
        stock.configurarMinimo(id(1), "Lima", 10);
        verificar("RF-04 configuracion de minimo", 10,
                stock.consultarDisponibilidad(id(1), "Lima").minimoConfigurado().orElseThrow());
        verificar("RF-05 alerta por debajo del minimo", true,
                stock.consultarAlertasActuales().stream().anyMatch(alerta -> alerta.categoriaId().equals(id(1))
                        && alerta.sede().equalsIgnoreCase("Lima")));
        esperar("RF-05 salida rechazada en mantenimiento", IllegalStateException.class,
                () -> movimientos.registrarSalida(id(901), "Destino", "No disponible"));
    }

    private void demostrarReporte() {
        var reporte = reportes.generarGlobal(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        verificar("RF-09 reporte incluye movimientos de la demo", true,
                reporte.movimientos().stream().anyMatch(movimiento -> movimiento.getEquipo().id().equals(id(901))));
        verificar("RF-09 separa saldo historico de existencias actuales", true,
                reporte.existenciasActuales() != null);
    }

    private void demostrarDuplicado() {
        esperar("RF-010 rechaza serie duplicada", IllegalArgumentException.class,
                () -> equipos.registrarEquipo(id(902), "DEMO-902", "DEMO-SERIE-901",
                        "Dell", "Equipo duplicado", id(1), id(11)));
        verificar("RF-010 rechazo no agrega el equipo", false,
                equipos.listar().stream().anyMatch(equipo -> equipo.id().equals(id(902))));
    }

    private void demostrarRoles() {
        autenticacion.cerrarSesion();
        autenticar(DatosIniciales.USUARIO_ALMACENERO, DatosIniciales.CLAVE_ALMACENERO);
        stock.configurarMinimo(id(1), "Lima", 2);
        verificar("RF-012 Almacenero configura minimo", 2,
                stock.consultarDisponibilidad(id(1), "Lima").minimoConfigurado().orElseThrow());
        autenticacion.cerrarSesion();

        autenticar(DatosIniciales.USUARIO_AUDITOR, DatosIniciales.CLAVE_AUDITOR);
        verificar("RF-012 Auditor consulta reportes", true,
                reportes.generarGlobal(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)) != null);
        esperar("RF-012 Auditor no configura minimo", SecurityException.class,
                () -> stock.configurarMinimo(id(1), "Lima", 1));
        esperar("RF-012 sin sesion rechaza inventario", SecurityException.class, () -> {
            autenticacion.cerrarSesion();
            equipos.listar();
        });
        autenticar(DatosIniciales.USUARIO_ADMIN, DatosIniciales.CLAVE_ADMIN);
        verificar("RF-012 Administrador configura minimo", true,
                autorizacion.tienePermiso(Permiso.CONFIGURAR_STOCK_MINIMO));
    }

    private void autenticar(String usuario, String clave) {
        autenticacion.iniciarSesion(usuario, clave.toCharArray());
    }

    private void verificar(String rf, Object esperado, Object obtenido) {
        boolean correcto = Objects.equals(esperado, obtenido);
        System.out.println((correcto ? "OK" : "FALLO") + " | " + rf
                + " | esperado=" + esperado + " | obtenido=" + obtenido);
        if (!correcto) {
            throw new IllegalStateException("Demo fallida: " + rf);
        }
        demostraciones++;
    }

    private void esperar(String rf, Class<? extends RuntimeException> esperada, Runnable operacion) {
        String obtenida = mensajeRechazo(operacion);
        verificar(rf, esperada.getSimpleName(), obtenida);
    }

    private String mensajeRechazo(Runnable operacion) {
        try {
            operacion.run();
            return "ninguna";
        } catch (RuntimeException excepcion) {
            return excepcion instanceof SecurityException
                    && "Credenciales incorrectas".equals(excepcion.getMessage())
                    ? excepcion.getMessage() : excepcion.getClass().getSimpleName();
        }
    }

    private UUID id(long valor) {
        return new UUID(0L, valor);
    }
}