package com.computototal.inventario.demo;

import com.computototal.inventario.dao.CategoriaDAO;
import com.computototal.inventario.dao.ConfiguracionStockMinimoDAO;
import com.computototal.inventario.dao.EquipoDAO;
import com.computototal.inventario.dao.MovimientoDAO;
import com.computototal.inventario.dao.ProveedorDAO;
import com.computototal.inventario.dao.UbicacionDAO;
import com.computototal.inventario.dao.UsuarioDAO;
import com.computototal.inventario.dao.memoria.CategoriaDAOMemoria;
import com.computototal.inventario.dao.memoria.ConfiguracionStockMinimoDAOMemoria;
import com.computototal.inventario.dao.memoria.EquipoDAOMemoria;
import com.computototal.inventario.dao.memoria.MovimientoDAOMemoria;
import com.computototal.inventario.dao.memoria.ProveedorDAOMemoria;
import com.computototal.inventario.dao.memoria.UbicacionDAOMemoria;
import com.computototal.inventario.dao.memoria.UsuarioDAOMemoria;
import com.computototal.inventario.modelo.Categoria;
import com.computototal.inventario.modelo.ConfiguracionStockMinimo;
import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.Proveedor;
import com.computototal.inventario.modelo.Rol;
import com.computototal.inventario.modelo.Ubicacion;
import com.computototal.inventario.modelo.Usuario;
import com.computototal.inventario.servicio.AutenticacionServicio;
import com.computototal.inventario.servicio.AutorizacionServicio;
import com.computototal.inventario.servicio.EquipoConsulta;
import com.computototal.inventario.servicio.EquipoServicio;
import com.computototal.inventario.servicio.EstadoStock;
import com.computototal.inventario.servicio.GestorContrasenas;
import com.computototal.inventario.servicio.MovimientoServicio;
import com.computototal.inventario.servicio.ReporteMovimientos;
import com.computototal.inventario.servicio.ReporteServicio;
import com.computototal.inventario.servicio.ResultadoMovimiento;
import com.computototal.inventario.servicio.SaldoCategoria;
import com.computototal.inventario.servicio.Sesion;
import com.computototal.inventario.servicio.StockServicio;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public final class VerificacionStockReportes {
    private static int comprobaciones;

    private VerificacionStockReportes() {
    }

    public static void main(String[] args) {
        verificarStockYAlertas();
        verificarReportes();
        System.out.println("RESULTADO: " + comprobaciones + " comprobaciones OK.");
    }

    private static void verificarStockYAlertas() {
        Contexto contexto = crearContexto();
        contexto.autenticacion().iniciarSesion("admin", claveAdmin());
        EquipoConsulta disponibleUno = alta(contexto, 101, "EQ-101", id(21));
        comparar("Alta de stock empieza presente en su almacen", true, disponibleUno.presenteEnAlmacen());
        alta(contexto, 102, "EQ-102", id(21));
        alta(contexto, 103, "EQ-103", id(21));
        EquipoConsulta fueraDeAlmacen = contexto.equipoServicio().registrarEquipo(
                id(104), "EQ-104", "SER-104", "Lenovo", "T14", id(1), id(11));
        contexto.movimientoServicio().cambiarEstado(id(104), EstadoEquipo.EN_USO, "Asignacion externa");
        contexto.movimientoServicio().cambiarEstado(id(102), EstadoEquipo.EN_MANTENIMIENTO, "Revision");
        contexto.movimientoServicio().cambiarEstado(id(103), EstadoEquipo.DE_BAJA, "Fin de vida");

        ConfiguracionStockMinimo configuracion = contexto.stockServicio().configurarMinimo(id(1), " lima ", 2);
        comparar("Configura minimo no negativo", 2, configuracion.getMinimo());
        contexto.stockServicio().configurarMinimo(id(1), "LIMA", 3);
        comparar("Sede normalizada actualiza la misma configuracion", 1,
                contexto.configuracionDAO().listar().size());
        comparar("El minimo actualizado se consulta con otra capitalizacion", 3,
                contexto.stockServicio().consultarDisponibilidad(id(1), " Lima ").minimoConfigurado().orElseThrow());
        contexto.stockServicio().configurarMinimo(id(1), "Lima", 2);
        esperar("Rechaza minimo negativo", IllegalArgumentException.class,
                () -> contexto.stockServicio().configurarMinimo(id(1), "Lima", -1));
        esperar("Rechaza categoria inexistente", NoSuchElementException.class,
                () -> contexto.stockServicio().configurarMinimo(id(999), "Lima", 1));
        esperar("Rechaza sede no representada en ubicaciones", NoSuchElementException.class,
                () -> contexto.stockServicio().configurarMinimo(id(1), "Cusco", 1));

        EstadoStock estado = contexto.stockServicio().consultarDisponibilidad(id(1), "LIMA");
        comparar("Cuenta equipos presentes incluyendo mantenimiento y baja", 3, estado.totalPresente());
        comparar("Disponible excluye mantenimiento y baja, y equipos externos", 1, estado.disponible());
        comparar("Stock por debajo del minimo produce alerta", true, estado.tieneAlerta());
        comparar("Equipo fuera del almacen no suma presencia", false, fueraDeAlmacen.presenteEnAlmacen());

        contexto.stockServicio().configurarMinimo(id(1), "Lima", 1);
        comparar("Igualdad con el minimo no alerta", false,
                contexto.stockServicio().consultarDisponibilidad(id(1), "Lima").tieneAlerta());
        contexto.stockServicio().configurarMinimo(id(1), "Lima", 0);
        comparar("Minimo cero nunca alerta con disponibilidad no negativa", false,
                contexto.stockServicio().consultarDisponibilidad(id(1), "Lima").tieneAlerta());
        contexto.stockServicio().configurarMinimo(id(1), "Lima", 2);

        contexto.categoriaDAO().insertar(new Categoria(id(2), "Monitores", "Pantallas"));
        contexto.stockServicio().configurarMinimo(id(2), "Lima", 1);
        EstadoStock cero = contexto.stockServicio().consultarDisponibilidad(id(2), "Lima");
        comparar("Minimo configurado sin equipos produce disponibilidad cero", 0, cero.disponible());
        comparar("Minimo configurado sin equipos aparece con total presente cero", 0, cero.totalPresente());
        comparar("Minimo configurado sin equipos genera alerta", true, cero.tieneAlerta());

        int configsAntesDeConsultar = contexto.configuracionDAO().listar().size();
        int alertasPrimeraConsulta = contexto.stockServicio().consultarAlertasActuales().size();
        int alertasSegundaConsulta = contexto.stockServicio().consultarAlertasActuales().size();
        comparar("Alertas se recalculan sin duplicar configuraciones", configsAntesDeConsultar,
                contexto.configuracionDAO().listar().size());
        comparar("Consultas repetidas devuelven el mismo numero de alertas actuales",
                alertasPrimeraConsulta, alertasSegundaConsulta);

        contexto.reloj().fijar(fechaHora(3, 8, 0));
        ResultadoMovimiento estadoMantenimiento = contexto.movimientoServicio()
                .cambiarEstado(id(101), EstadoEquipo.EN_MANTENIMIENTO, "Diagnostico");
        comparar("Cambio de estado actualiza automaticamente disponibilidad afectada", 0,
                estadoAfectado(estadoMantenimiento, id(1), "Lima").disponible());
        comparar("Cambio de estado devuelve alerta actual afectada", true,
                estadoAfectado(estadoMantenimiento, id(1), "Lima").tieneAlerta());

        contexto.reloj().fijar(fechaHora(4, 8, 0));
        ResultadoMovimiento restaurado = contexto.movimientoServicio()
                .cambiarEstado(id(101), EstadoEquipo.DISPONIBLE, "Mantenimiento concluido");
        comparar("Restablecer estado recalcula disponibilidad actual", 1,
                estadoAfectado(restaurado, id(1), "Lima").disponible());

        contexto.reloj().fijar(fechaHora(5, 8, 0));
        ResultadoMovimiento salida = contexto.movimientoServicio()
                .registrarSalida(id(101), "Mesa de soporte", "Entrega temporal");
        comparar("Salida recalcula alertas sin generar notificacion historica", true,
                estadoAfectado(salida, id(1), "Lima").tieneAlerta());

        contexto.reloj().fijar(fechaHora(6, 8, 0));
        ResultadoMovimiento ingreso = contexto.movimientoServicio()
                .registrarIngreso(id(101), id(21), "Devolucion");
        comparar("Reingreso vuelve a reflejar la disponibilidad", 1,
                estadoAfectado(ingreso, id(1), "Lima").disponible());

        contexto.stockServicio().configurarMinimo(id(1), "Arequipa", 1);
        contexto.reloj().fijar(fechaHora(7, 8, 0));
        ResultadoMovimiento traslado = contexto.movimientoServicio()
                .trasladar(id(101), id(24), "Traslado a Arequipa");
        comparar("Traslado devuelve la reevaluacion de ambas sedes", 2,
                traslado.estadosStockAfectados().size());
        comparar("Traslado reduce el origen sin cambiar el total global", 0,
                estadoAfectado(traslado, id(1), "Lima").disponible());
        comparar("Traslado aumenta el destino segun su minimo", 1,
                estadoAfectado(traslado, id(1), "Arequipa").disponible());
        comparar("Traslado notifica alerta del origen y no alerta en igualdad de destino", true,
                estadoAfectado(traslado, id(1), "Lima").tieneAlerta()
                        && !estadoAfectado(traslado, id(1), "Arequipa").tieneAlerta());

        contexto.autenticacion().cerrarSesion();
        contexto.autenticacion().iniciarSesion("almacen", claveAlmacenero());
        contexto.stockServicio().configurarMinimo(id(1), "Lima", 4);
        comparar("Almacenero configura minimos", 4,
                contexto.stockServicio().consultarDisponibilidad(id(1), "Lima").minimoConfigurado().orElseThrow());
        contexto.autenticacion().cerrarSesion();
        contexto.autenticacion().iniciarSesion("auditor", claveAuditor());
        comparar("Auditor consulta existencias", true, !contexto.stockServicio().listarExistencias().isEmpty());
        comparar("Auditor consulta alertas", true, contexto.stockServicio().consultarAlertasActuales() != null);
        esperar("Auditor no configura minimos", SecurityException.class,
                () -> contexto.stockServicio().configurarMinimo(id(1), "Lima", 1));
        contexto.autenticacion().cerrarSesion();
        esperar("Stock sin sesion se rechaza", SecurityException.class,
                () -> contexto.stockServicio().listarExistencias());
    }

    private static void verificarReportes() {
        Contexto contexto = crearContexto();
        contexto.autenticacion().iniciarSesion("admin", claveAdmin());

        contexto.reloj().fijar(fechaHora(31, 23, 59));
        alta(contexto, 201, "EQ-201", id(21));
        alta(contexto, 202, "EQ-202", id(21));

        contexto.reloj().fijar(fechaHora(1, 0, 0, 6));
        contexto.movimientoServicio().trasladar(id(201), id(24), "Traslado del periodo");
        contexto.reloj().fijar(fechaHora(15, 12, 0, 6));
        contexto.movimientoServicio().registrarSalida(id(202), "Oficina externa", "Entrega durante periodo");
        contexto.reloj().fijar(fechaHora(30, 23, 59, 6));
        contexto.movimientoServicio().registrarIngreso(id(202), id(21), "Reingreso al cierre");

        ReporteMovimientos global = contexto.reporteServicio().generarGlobal(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        comparar("Reporte global incluye movimientos de todo el ultimo dia", 3, global.movimientos().size());
        comparar("Reporte global incluye el movimiento de 23:59 del ultimo dia", true,
                global.movimientos().stream().anyMatch(movimiento -> movimiento.getFechaHora().toLocalDate()
                        .equals(LocalDate.of(2026, 6, 30))));
        comparar("Reporte global resume entradas y salidas", true,
                global.entradas() == 1 && global.salidas() == 1);
        comparar("Reporte global distingue traslados recibidos y enviados", true,
                global.trasladosRecibidos() == 1 && global.trasladosEnviados() == 1);
        comparar("Saldo inicial global reconstruido", 2, global.saldoInicial());
        comparar("Saldo final global conserva traslados", 2, global.saldoFinal());
        SaldoCategoria saldoGlobal = obtenerSaldo(global, id(1));
        comparar("Agrupa saldo historico por categoria id", "Laptops", saldoGlobal.categoria());
        comparar("Saldo por categoria reconstruido", true,
                saldoGlobal.saldoInicial() == 2 && saldoGlobal.entradas() == 1
                        && saldoGlobal.salidas() == 1 && saldoGlobal.saldoFinal() == 2);

        ReporteMovimientos lima = contexto.reporteServicio().generarPorSede(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), " lima ");
        comparar("Saldo inicial de Lima por sede", 2, lima.saldoInicial());
        comparar("Saldo final de Lima respeta traslados y flujos", 1, lima.saldoFinal());
        comparar("Ecuacion por sede de Lima se cumple", lima.saldoInicial() + lima.entradas() - lima.salidas()
                        + lima.trasladosRecibidos() - lima.trasladosEnviados(), lima.saldoFinal());
        comparar("Reporte Lima cuenta traslado enviado", 1, lima.trasladosEnviados());

        ReporteMovimientos arequipa = contexto.reporteServicio().generarPorSede(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "AREQUIPA");
        comparar("Saldo final de Arequipa recibe el traslado", 1, arequipa.saldoFinal());
        comparar("Ecuacion por sede de Arequipa se cumple", arequipa.saldoInicial() + arequipa.entradas()
                        - arequipa.salidas() + arequipa.trasladosRecibidos() - arequipa.trasladosEnviados(),
                arequipa.saldoFinal());
        comparar("Los traslados no alteran el saldo global", lima.saldoFinal() + arequipa.saldoFinal(),
                global.saldoFinal());

        ReporteMovimientos vacio = contexto.reporteServicio().generarGlobal(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2));
        comparar("Rango sin movimientos devuelve detalle vacio", 0, vacio.movimientos().size());
        comparar("Rango sin movimientos conserva saldo previo", true,
                vacio.saldoInicial() == 2 && vacio.saldoFinal() == 2);
        esperar("Reporte rechaza fechas invertidas", IllegalArgumentException.class,
                () -> contexto.reporteServicio().generarGlobal(
                        LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 1)));

        contexto.stockServicio().configurarMinimo(id(1), "Lima", 0);
        contexto.stockServicio().configurarMinimo(id(1), "Arequipa", 0);
        contexto.reloj().fijar(fechaHora(1, 9, 0, 8));
        alta(contexto, 203, "EQ-203", id(21));
        ReporteMovimientos junioDespuesDeCambios = contexto.reporteServicio().generarGlobal(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        comparar("Movimiento futuro no altera saldo historico final", 2, junioDespuesDeCambios.saldoFinal());
        comparar("Stock actual se presenta como dato separado del saldo historico", true,
                junioDespuesDeCambios.saldoFinal() == 2
                        && junioDespuesDeCambios.existenciasActuales().stream()
                        .mapToInt(EstadoStock::disponible).sum() == 3);

        Categoria categoria = contexto.categoriaDAO().buscarPorId(id(1)).orElseThrow();
        categoria.setNombre("Portatiles actualizados");
        contexto.categoriaDAO().actualizar(categoria);
        Ubicacion ubicacion = contexto.ubicacionDAO().buscarPorId(id(21)).orElseThrow();
        ubicacion.setSede("Lima nueva");
        contexto.ubicacionDAO().actualizar(ubicacion);
        comparar("Cambios actuales de catalogo no reescriben reporte previo", true,
                global.saldoFinal() == 2 && obtenerSaldo(global, id(1)).categoria().equals("Laptops")
                        && global.movimientos().get(0).getUbicacionOrigen().orElseThrow().sede().equals("Lima"));

        contexto.autenticacion().cerrarSesion();
        esperar("Reporte sin sesion se rechaza", SecurityException.class,
                () -> contexto.reporteServicio().generarGlobal(
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));
    }

    private static SaldoCategoria obtenerSaldo(ReporteMovimientos reporte, UUID categoriaId) {
        return reporte.saldosPorCategoria().stream()
                .filter(saldo -> saldo.categoriaId().equals(categoriaId))
                .findFirst()
                .orElseThrow();
    }

    private static EstadoStock estadoAfectado(ResultadoMovimiento resultado, UUID categoriaId, String sede) {
        return resultado.estadosStockAfectados().stream()
                .filter(estado -> estado.categoriaId().equals(categoriaId)
                        && estado.sede().equalsIgnoreCase(sede))
                .findFirst()
                .orElseThrow();
    }

    private static EquipoConsulta alta(Contexto contexto, long numero, String codigo, UUID ubicacionId) {
        return contexto.equipoServicio().registrarEquipoConIngreso(id(numero), codigo, "SER-" + numero,
                "Lenovo", "T14", id(1), id(11), ubicacionId, "Ingreso de demostracion").equipo();
    }

    private static Contexto crearContexto() {
        EquipoDAO equipoDAO = new EquipoDAOMemoria();
        MovimientoDAO movimientoDAO = new MovimientoDAOMemoria();
        CategoriaDAO categoriaDAO = new CategoriaDAOMemoria();
        ConfiguracionStockMinimoDAO configuracionDAO = new ConfiguracionStockMinimoDAOMemoria();
        ProveedorDAO proveedorDAO = new ProveedorDAOMemoria();
        UbicacionDAO ubicacionDAO = new UbicacionDAOMemoria();
        UsuarioDAO usuarioDAO = new UsuarioDAOMemoria();
        categoriaDAO.insertar(new Categoria(id(1), "Laptops", "Equipos portatiles"));
        proveedorDAO.insertar(new Proveedor(id(11), "Proveedor Uno", "999111222", "contacto@uno.pe"));
        ubicacionDAO.insertar(new Ubicacion(id(21), "Lima", "Almacen Lima", "Inventario", "PB", true));
        ubicacionDAO.insertar(new Ubicacion(id(22), "Lima", "Almacen Lima B", "Inventario", "Sotano", true));
        ubicacionDAO.insertar(new Ubicacion(id(23), "Lima", "Operaciones", "Soporte", "2", false));
        ubicacionDAO.insertar(new Ubicacion(id(24), "Arequipa", "Almacen Arequipa", "Inventario", "PB", true));

        GestorContrasenas gestor = new GestorContrasenas();
        insertarUsuario(usuarioDAO, gestor, id(31), "admin", Rol.ADMINISTRADOR, claveAdmin());
        insertarUsuario(usuarioDAO, gestor, id(32), "almacen", Rol.ALMACENERO, claveAlmacenero());
        insertarUsuario(usuarioDAO, gestor, id(33), "auditor", Rol.AUDITOR, claveAuditor());
        Sesion sesion = new Sesion();
        AutenticacionServicio autenticacion = new AutenticacionServicio(usuarioDAO, gestor, sesion);
        AutorizacionServicio autorizacion = new AutorizacionServicio(sesion);
        RelojControlado reloj = new RelojControlado(fechaHora(1, 8, 0), ZoneId.of("UTC"));
        StockServicio stockServicio = new StockServicio(categoriaDAO, configuracionDAO, equipoDAO,
                ubicacionDAO, sesion, autorizacion);
        MovimientoServicio movimientoServicio = new MovimientoServicio(equipoDAO, movimientoDAO,
                categoriaDAO, proveedorDAO, ubicacionDAO, sesion, autorizacion, stockServicio, reloj);
        EquipoServicio equipoServicio = new EquipoServicio(equipoDAO, categoriaDAO, proveedorDAO, ubicacionDAO,
                movimientoDAO, movimientoServicio, sesion, autorizacion);
        ReporteServicio reporteServicio = new ReporteServicio(movimientoDAO, stockServicio, sesion, autorizacion);
        return new Contexto(equipoDAO, movimientoDAO, categoriaDAO, configuracionDAO, proveedorDAO,
                ubicacionDAO, sesion, autenticacion, autorizacion, stockServicio, movimientoServicio,
                equipoServicio, reporteServicio, reloj);
    }

    private static void insertarUsuario(UsuarioDAO dao, GestorContrasenas gestor, UUID id,
                                        String nombre, Rol rol, char[] contrasena) {
        GestorContrasenas.Credencial credencial = gestor.generarHash(contrasena);
        dao.insertar(new Usuario(id, nombre, credencial.getHashContrasena(), credencial.getSalContrasena(), rol));
    }

    private static LocalDateTime fechaHora(int dia, int hora, int minuto) {
        return fechaHora(dia, hora, minuto, 5);
    }

    private static LocalDateTime fechaHora(int dia, int hora, int minuto, int mes) {
        return LocalDateTime.of(2026, mes, dia, hora, minuto);
    }

    private static UUID id(long valor) {
        return new UUID(0L, valor);
    }

    private static char[] claveAdmin() {
        return "demo-admin-stock".toCharArray();
    }

    private static char[] claveAlmacenero() {
        return "demo-almacen-stock".toCharArray();
    }

    private static char[] claveAuditor() {
        return "demo-auditor-stock".toCharArray();
    }

    private static void esperar(String descripcion, Class<? extends RuntimeException> esperada, Runnable operacion) {
        String obtenida = "ninguna";
        try {
            operacion.run();
        } catch (RuntimeException excepcion) {
            obtenida = excepcion.getClass().getSimpleName();
        }
        comparar(descripcion, esperada.getSimpleName(), obtenida);
    }

    private static void comparar(String descripcion, Object esperado, Object obtenido) {
        boolean correcto = Objects.equals(esperado, obtenido);
        System.out.println((correcto ? "OK" : "FALLO") + " | " + descripcion
                + " | esperado=" + esperado + " | obtenido=" + obtenido);
        if (!correcto) {
            throw new IllegalStateException("Verificacion fallida: " + descripcion);
        }
        comprobaciones++;
    }

    private record Contexto(EquipoDAO equipoDAO, MovimientoDAO movimientoDAO, CategoriaDAO categoriaDAO,
                            ConfiguracionStockMinimoDAO configuracionDAO, ProveedorDAO proveedorDAO,
                            UbicacionDAO ubicacionDAO, Sesion sesion, AutenticacionServicio autenticacion,
                            AutorizacionServicio autorizacion, StockServicio stockServicio,
                            MovimientoServicio movimientoServicio, EquipoServicio equipoServicio,
                            ReporteServicio reporteServicio, RelojControlado reloj) {
    }

    private static final class RelojControlado extends Clock {
        private final ZoneId zona;
        private Instant instante;

        private RelojControlado(LocalDateTime fecha, ZoneId zona) {
            this.zona = zona;
            fijar(fecha);
        }

        private void fijar(LocalDateTime fecha) {
            instante = fecha.atZone(zona).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return zona;
        }

        @Override
        public Clock withZone(ZoneId zonaNueva) {
            return Clock.fixed(instante, zonaNueva);
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}