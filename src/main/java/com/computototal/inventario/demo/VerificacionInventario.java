package com.computototal.inventario.demo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.Movimiento;
import com.computototal.inventario.modelo.Proveedor;
import com.computototal.inventario.modelo.Rol;
import com.computototal.inventario.modelo.TipoMovimiento;
import com.computototal.inventario.modelo.Ubicacion;
import com.computototal.inventario.modelo.Usuario;
import com.computototal.inventario.servicio.AutenticacionServicio;
import com.computototal.inventario.servicio.AutorizacionServicio;
import com.computototal.inventario.servicio.EquipoConsulta;
import com.computototal.inventario.servicio.EquipoServicio;
import com.computototal.inventario.servicio.GestorContrasenas;
import com.computototal.inventario.servicio.MovimientoServicio;
import com.computototal.inventario.servicio.ResultadoAltaEquipo;
import com.computototal.inventario.servicio.ResultadoMovimiento;
import com.computototal.inventario.servicio.Sesion;
import com.computototal.inventario.servicio.StockServicio;
import com.computototal.inventario.validacion.ValidacionesEntrada;

public final class VerificacionInventario {
    private static int comprobaciones;

    private VerificacionInventario() {
    }

    public static void main(String[] args) {
        Contexto contexto = crearContexto();
        contexto.autenticacion().iniciarSesion("admin", contrasenaAdmin());

        verificarRegistroYConsultas(contexto);
        verificarCambiosYSalidas(contexto);
        verificarValidacionesYFechas(contexto);
        verificarSnapshots(contexto);
        verificarRolesYEliminacion(contexto);
        verificarCompensacion(contexto);

        contexto.autenticacion().cerrarSesion();
        System.out.println("RESULTADO: " + comprobaciones + " comprobaciones OK.");
    }

    private static void verificarRegistroYConsultas(Contexto contexto) {
        contexto.reloj().fijar(fecha(1, 10, 0));
        ResultadoAltaEquipo alta = contexto.equipoServicio().registrarEquipoConIngreso(
                id(101), "EQ-101", "SER-101", "Lenovo", "T14", id(1), id(11), id(21), "Alta inicial");
        EquipoConsulta equipo = alta.equipo();
        List<Movimiento> historialInicial = contexto.movimientoServicio().historialPorEquipo(equipo.id());
        comparar("Alta con almacen crea exactamente un ingreso", 1, historialInicial.size());
        comparar("Alta con almacen registra ingreso", TipoMovimiento.INGRESO, historialInicial.get(0).getTipo());
        comparar("Alta con almacen deja disponible y presente", true,
                equipo.estado() == EstadoEquipo.DISPONIBLE && equipo.presenteEnAlmacen());
        comparar("El historial captura el responsable de la sesion", "admin", historialInicial.get(0).getNombreResponsable());
        comparar("El alta conserva el motivo", "Alta inicial", historialInicial.get(0).getMotivo());

        EquipoConsulta fueraDelAlmacen = contexto.equipoServicio().registrarEquipo(
                id(102), "EQ-102", "SER-102", "Dell", "P1", id(1), id(11));
        comparar("Registro sin ingreso empieza fuera del almacen", false, fueraDelAlmacen.presenteEnAlmacen());
        comparar("Registro sin ingreso no crea historial", 0,
                contexto.movimientoServicio().historialPorEquipo(id(102)).size());
        contexto.movimientoServicio().registrarIngreso(id(102), id(21), "Ingreso posterior");
        comparar("Ingreso posterior registra un evento", 1,
                contexto.movimientoServicio().historialPorEquipo(id(102)).size());

        int totalMovimientos = contexto.movimientoDAO().listar().size();
        esperar("Rechaza codigo duplicado sin alta parcial", IllegalArgumentException.class,
                () -> contexto.equipoServicio().registrarEquipo(id(103), " eq-101 ", "SER-103",
                        "HP", "EliteBook", id(1), id(11)));
        esperar("Rechaza serie duplicada sin alta parcial", IllegalArgumentException.class,
                () -> contexto.equipoServicio().registrarEquipo(id(104), "EQ-104", "ser-101",
                        "HP", "EliteBook", id(1), id(11)));
        comparar("Duplicados no insertan equipos", false,
                contieneEquipo(contexto.equipoServicio().listar(), id(103))
                        || contieneEquipo(contexto.equipoServicio().listar(), id(104)));
        comparar("Duplicados no agregan movimientos", totalMovimientos, contexto.movimientoDAO().listar().size());

        esperar("Rechaza categoria inexistente", NoSuchElementException.class,
                () -> contexto.equipoServicio().registrarEquipo(id(105), "EQ-105", "SER-105",
                        "HP", "EliteBook", id(900), id(11)));
        esperar("Rechaza proveedor inexistente", NoSuchElementException.class,
                () -> contexto.equipoServicio().registrarEquipo(id(105), "EQ-105", "SER-105",
                        "HP", "EliteBook", id(1), id(911)));
        comparar("Referencias inexistentes no dejan equipos", false,
                contieneEquipo(contexto.equipoServicio().listar(), id(105)));

        EquipoConsulta porCodigo = contexto.equipoServicio().consultarPorCodigo(" eq-101 ");
        EquipoConsulta porSerie = contexto.equipoServicio().consultarPorNumeroSerie(" SER-101 ");
        comparar("Consulta por codigo resuelve identidad", id(101), porCodigo.id());
        comparar("Consulta por serie resuelve identidad", id(101), porSerie.id());
        comparar("Consulta expone ubicacion actual", "Almacen Lima A",
                porCodigo.ubicacionActual().orElseThrow().ambiente());
        comparar("Lista de equipos devuelve resultado independiente", 2, contexto.equipoServicio().listar().size());
        comparar("La serie se recorta y normaliza a mayusculas", "SN-007",
                ValidacionesEntrada.numeroSerie(" sn-007 "));
        esperar("Rechaza serie menor a tres caracteres", IllegalArgumentException.class,
                () -> contexto.equipoServicio().registrarEquipo(id(110), "EQ-SER-110", "A1",
                        "HP", "ProBook", id(1), id(11)));
        esperar("Rechaza espacios dentro de la serie", IllegalArgumentException.class,
                () -> contexto.equipoServicio().registrarEquipo(id(111), "EQ-SER-111", "SN 123",
                        "HP", "ProBook", id(1), id(11)));
        esperar("Rechaza simbolos no permitidos en la serie", IllegalArgumentException.class,
                () -> contexto.equipoServicio().registrarEquipo(id(112), "EQ-SER-112", "SN#123",
                        "HP", "ProBook", id(1), id(11)));
        esperar("Rechaza serie mayor a cuarenta caracteres", IllegalArgumentException.class,
                () -> contexto.equipoServicio().registrarEquipo(id(113), "EQ-SER-113", "A".repeat(41),
                        "HP", "ProBook", id(1), id(11)));

        contexto.reloj().fijar(fecha(1, 10, 0));
        contexto.movimientoServicio().cambiarUbicacion(id(101), id(22), "Reorganizacion interna");
        List<Movimiento> historialMismaFecha = contexto.movimientoServicio().historialPorEquipo(id(101));
        comparar("Fechas iguales conservan el orden real de insercion",
                List.of(TipoMovimiento.INGRESO, TipoMovimiento.CAMBIO_UBICACION),
                historialMismaFecha.stream().map(Movimiento::getTipo).toList());
        comparar("Cambio de ubicacion conserva instantaneas", "Almacen Lima A",
                historialMismaFecha.get(1).getUbicacionOrigen().orElseThrow().ambiente());
        comparar("Cambio de ubicacion registra destino", "Almacen Lima B",
                historialMismaFecha.get(1).getUbicacionDestino().orElseThrow().ambiente());
    }

    private static void verificarCambiosYSalidas(Contexto contexto) {
        contexto.reloj().fijar(fecha(1, 10, 1));
        EquipoConsulta equipo = contexto.equipoServicio().consultarPorId(id(101));
        contexto.movimientoServicio().cambiarUbicacion(id(101), id(23), "Traslado interno a operaciones");
        EquipoConsulta ubicacionOperativa = contexto.equipoServicio().consultarPorId(id(101));
        comparar("Ubicacion operativa deja de estar presente en almacen", false,
                ubicacionOperativa.presenteEnAlmacen());
        comparar("Cambio de ubicacion conserva ultima ubicacion de almacen", id(22),
                ubicacionOperativa.ultimaUbicacionAlmacen().orElseThrow().id());
        comparar("Cambio de ubicacion conserva estado tecnico", equipo.estado(), ubicacionOperativa.estado());

        contexto.reloj().fijar(fecha(1, 10, 2));
        contexto.movimientoServicio().registrarIngreso(id(101), id(21), "Reingreso desde operaciones");
        comparar("Reingreso limpia destino y establece disponible", true,
                contexto.equipoServicio().consultarPorId(id(101)).estado() == EstadoEquipo.DISPONIBLE
                        && contexto.equipoServicio().consultarPorId(id(101)).destinoSalida().isEmpty());

        contexto.reloj().fijar(fecha(1, 10, 3));
                Movimiento traslado = contexto.movimientoServicio().trasladar(id(101), id(24), "Traslado intersede").movimiento();
        comparar("Cambio entre sedes usa tipo TRASLADO", TipoMovimiento.TRASLADO, traslado.getTipo());
        comparar("Traslado mantiene disponible y presente", true,
                traslado.getEstadoAnterior().orElseThrow() == EstadoEquipo.DISPONIBLE
                        && traslado.getEstadoNuevo().orElseThrow() == EstadoEquipo.DISPONIBLE
                        && traslado.isPresenciaAnterior() && traslado.isPresenciaNueva());
        comparar("Traslado registra sedes distintas", true,
                !traslado.getUbicacionOrigen().orElseThrow().sede()
                        .equals(traslado.getUbicacionDestino().orElseThrow().sede()));

        contexto.reloj().fijar(fecha(1, 10, 4));
        Movimiento salida = contexto.movimientoServicio()
                .registrarSalida(id(101), "Sede externa", "Entrega a usuario").movimiento();
        comparar("Salida establece EN_USO y retira presencia", true,
                salida.getEstadoAnterior().orElseThrow() == EstadoEquipo.DISPONIBLE
                        && salida.getEstadoNuevo().orElseThrow() == EstadoEquipo.EN_USO
                        && salida.isPresenciaAnterior() && !salida.isPresenciaNueva());
        comparar("Salida conserva destino declarado", "Sede externa", salida.getDestinoExterno().orElseThrow());
        comparar("Salida conserva ultima ubicacion de almacen", id(24),
                contexto.equipoServicio().consultarPorId(id(101)).ultimaUbicacionAlmacen().orElseThrow().id());

        contexto.reloj().fijar(fecha(1, 10, 5));
        Movimiento reingreso = contexto.movimientoServicio()
                .registrarIngreso(id(101), id(24), "Devolucion al almacen").movimiento();
        comparar("Reingreso cambia EN_USO a DISPONIBLE", true,
                reingreso.getEstadoAnterior().orElseThrow() == EstadoEquipo.EN_USO
                        && reingreso.getEstadoNuevo().orElseThrow() == EstadoEquipo.DISPONIBLE);

        contexto.reloj().fijar(fecha(1, 10, 6));
        contexto.movimientoServicio().cambiarEstado(id(101), EstadoEquipo.EN_MANTENIMIENTO, "Diagnostico");
        int movimientosEnMantenimiento = contexto.movimientoDAO().listar().size();
        esperar("Mantenimiento bloquea salida", IllegalStateException.class,
                () -> contexto.movimientoServicio().registrarSalida(id(101), "Taller", "Reparacion"));
        esperar("Mantenimiento bloquea traslado", IllegalStateException.class,
                () -> contexto.movimientoServicio().trasladar(id(101), id(21), "Reubicacion"));
        comparar("Operaciones bloqueadas no cambian equipo ni historial", true,
                contexto.equipoServicio().consultarPorId(id(101)).estado() == EstadoEquipo.EN_MANTENIMIENTO
                        && contexto.movimientoDAO().listar().size() == movimientosEnMantenimiento);

        contexto.reloj().fijar(fecha(1, 10, 7));
        contexto.movimientoServicio().cambiarEstado(id(101), EstadoEquipo.DISPONIBLE, "Mantenimiento concluido");
        contexto.reloj().fijar(fecha(1, 10, 8));
        contexto.movimientoServicio().cambiarEstado(id(101), EstadoEquipo.DE_BAJA, "Fin de vida util");
        int movimientosDeBaja = contexto.movimientoDAO().listar().size();
        esperar("DE_BAJA es terminal para cambio de estado", IllegalStateException.class,
                () -> contexto.movimientoServicio().cambiarEstado(id(101), EstadoEquipo.EN_USO, "Reactivar"));
        esperar("DE_BAJA bloquea movimientos de salida", IllegalStateException.class,
                () -> contexto.movimientoServicio().registrarSalida(id(101), "Destino", "Salida"));
        comparar("Intentos sobre DE_BAJA no crean movimientos", movimientosDeBaja,
                contexto.movimientoDAO().listar().size());
    }

    private static void verificarValidacionesYFechas(Contexto contexto) {
        EquipoConsulta equipo = contexto.equipoServicio().consultarPorId(id(102));
        int movimientosAntes = contexto.movimientoServicio().historialPorEquipo(id(102)).size();

        esperar("Salida exige motivo", IllegalArgumentException.class,
                () -> contexto.movimientoServicio().registrarSalida(id(102), "Destino", " "));
        esperar("Salida exige destino", IllegalArgumentException.class,
                () -> contexto.movimientoServicio().registrarSalida(id(102), " ", "Entrega"));
        esperar("Traslado exige motivo", IllegalArgumentException.class,
                () -> contexto.movimientoServicio().trasladar(id(102), id(24), " "));
        esperar("Cambio de ubicacion exige motivo", IllegalArgumentException.class,
                () -> contexto.movimientoServicio().cambiarUbicacion(id(102), id(22), " "));
        esperar("Cambio de estado exige justificacion", IllegalArgumentException.class,
                () -> contexto.movimientoServicio().cambiarEstado(id(102), EstadoEquipo.EN_MANTENIMIENTO, " "));
        esperar("Ubicacion identica se rechaza", IllegalArgumentException.class,
                () -> contexto.movimientoServicio().cambiarUbicacion(id(102), id(21), "Sin cambio"));
        esperar("Cambio de sede no puede registrarse como cambio de ubicacion", IllegalArgumentException.class,
                () -> contexto.movimientoServicio().cambiarUbicacion(id(102), id(24), "Cambio de sede"));
        esperar("Cambio de estado identico se rechaza", IllegalArgumentException.class,
                () -> contexto.movimientoServicio().cambiarEstado(id(102), equipo.estado(), "Sin cambio"));
        esperar("Equipo presente no puede reingresar", IllegalStateException.class,
                () -> contexto.movimientoServicio().registrarIngreso(id(102), id(21), "Ingreso duplicado"));

        comparar("Validaciones fallidas no cambian equipo ni historial", true,
                contexto.equipoServicio().consultarPorId(id(102)).equals(equipo)
                        && contexto.movimientoServicio().historialPorEquipo(id(102)).size() == movimientosAntes);

        esperar("Equipo inexistente al consultar historial produce error claro", NoSuchElementException.class,
                () -> contexto.movimientoServicio().historialPorEquipo(id(999)));
        esperar("Codigo inexistente en historial produce error claro", NoSuchElementException.class,
                () -> contexto.movimientoServicio().historialPorCodigo("EQ-no-existe"));
        esperar("Serie inexistente en historial produce error claro", NoSuchElementException.class,
                () -> contexto.movimientoServicio().historialPorNumeroSerie("SER-no-existe"));

        contexto.reloj().fijar(fecha(1, 9, 59));
        int historialAntesDeFechaInvalida = contexto.movimientoServicio().historialPorEquipo(id(102)).size();
        esperar("Rechaza fecha anterior al ultimo movimiento", IllegalArgumentException.class,
                () -> contexto.movimientoServicio().cambiarEstado(id(102), EstadoEquipo.EN_MANTENIMIENTO,
                        "Fecha retroactiva"));
        comparar("Fecha retroactiva no altera equipo ni historial", true,
                contexto.equipoServicio().consultarPorId(id(102)).estado() == EstadoEquipo.DISPONIBLE
                        && contexto.movimientoServicio().historialPorEquipo(id(102)).size() == historialAntesDeFechaInvalida);
        contexto.reloj().fijar(fecha(1, 10, 9));
    }

    private static void verificarSnapshots(Contexto contexto) {
        List<Movimiento> historial = contexto.movimientoServicio().historialPorCodigo("EQ-101");
        Movimiento ingreso = historial.stream()
                .filter(movimiento -> movimiento.getTipo() == TipoMovimiento.INGRESO)
                .findFirst().orElseThrow();
        comparar("Historial por codigo encuentra equipo", id(101), ingreso.getEquipo().id());
        comparar("Instantanea conserva categoria original", "Laptops", ingreso.getEquipo().nombreCategoria());
        comparar("Instantanea conserva proveedor original", "Proveedor Uno", ingreso.getEquipo().nombreProveedor());
        comparar("Instantanea conserva sede original", "Lima", ingreso.getUbicacionDestino().orElseThrow().sede());
        comparar("Historial por serie devuelve los mismos eventos", historial.size(),
                contexto.movimientoServicio().historialPorNumeroSerie("SER-101").size());

        Categoria categoria = contexto.categoriaDAO().buscarPorId(id(1)).orElseThrow();
        categoria.setNombre("Laptops renombradas");
        contexto.categoriaDAO().actualizar(categoria);
        Proveedor proveedor = contexto.proveedorDAO().buscarPorId(id(11)).orElseThrow();
        proveedor.setNombre("Proveedor editado");
        contexto.proveedorDAO().actualizar(proveedor);
        Ubicacion ubicacion = contexto.ubicacionDAO().buscarPorId(id(21)).orElseThrow();
        ubicacion.setSede("Lima actualizada");
        ubicacion.setAmbiente("Almacen renombrado");
        contexto.ubicacionDAO().actualizar(ubicacion);

        Movimiento ingresoPersistido = contexto.movimientoServicio().historialPorEquipo(id(101)).stream()
                .filter(movimiento -> movimiento.getId().equals(ingreso.getId())).findFirst().orElseThrow();
        comparar("Editar catalogo no reescribe categoria del historial", "Laptops",
                ingresoPersistido.getEquipo().nombreCategoria());
        comparar("Editar catalogo no reescribe proveedor del historial", "Proveedor Uno",
                ingresoPersistido.getEquipo().nombreProveedor());
        comparar("Editar ubicacion no reescribe instantanea historica", "Lima",
                ingresoPersistido.getUbicacionDestino().orElseThrow().sede());
    }

    private static void verificarRolesYEliminacion(Contexto contexto) {
        contexto.autenticacion().cerrarSesion();
        contexto.autenticacion().iniciarSesion("almacen", contrasenaAlmacenero());
        EquipoConsulta creadoPorAlmacenero = contexto.equipoServicio().registrarEquipo(
                id(105), "EQ-105", "SER-105", "Acer", "TravelMate", id(1), id(11));
        comparar("Almacenero puede registrar equipo", id(105), creadoPorAlmacenero.id());
        contexto.movimientoServicio().cambiarEstado(id(102), EstadoEquipo.EN_MANTENIMIENTO, "Revision almacenero");
        comparar("Almacenero puede cambiar estado", EstadoEquipo.EN_MANTENIMIENTO,
                contexto.equipoServicio().consultarPorId(id(102)).estado());
        esperar("Almacenero no puede eliminar", SecurityException.class,
                () -> contexto.equipoServicio().eliminar(id(105)));
        esperar("Almacenero no puede anular altas", SecurityException.class,
                () -> contexto.equipoServicio().anularAlta(id(105), "Error de registro"));
        contexto.autenticacion().cerrarSesion();

        contexto.autenticacion().iniciarSesion("auditor", contrasenaAuditor());
        comparar("Auditor puede consultar inventario", true,
                contexto.equipoServicio().consultarPorNumeroSerie("SER-101").id().equals(id(101)));
        comparar("Auditor puede consultar historial", true,
                !contexto.movimientoServicio().historialPorEquipo(id(101)).isEmpty());
        int movimientosAntesAuditor = contexto.movimientoDAO().listar().size();
        esperar("Auditor no puede cambiar estado", SecurityException.class,
                () -> contexto.movimientoServicio().cambiarEstado(id(102), EstadoEquipo.DISPONIBLE,
                        "Intento de auditor"));
        esperar("Auditor no puede eliminar", SecurityException.class,
                () -> contexto.equipoServicio().eliminar(id(105)));
        esperar("Auditor no puede anular altas", SecurityException.class,
                () -> contexto.equipoServicio().anularAlta(id(105), "Error de registro"));
        comparar("Auditor no modifica equipo ni historial", true,
                contexto.equipoServicio().consultarPorId(id(102)).estado() == EstadoEquipo.EN_MANTENIMIENTO
                        && contexto.movimientoDAO().listar().size() == movimientosAntesAuditor);
        contexto.autenticacion().cerrarSesion();

        esperar("Consulta de inventario exige sesion", SecurityException.class,
                () -> contexto.equipoServicio().listar());
        esperar("Movimiento exige sesion", SecurityException.class,
                () -> contexto.movimientoServicio().registrarIngreso(id(105), id(21), "Sin sesion"));
        contexto.autenticacion().iniciarSesion("admin", contrasenaAdmin());

        contexto.equipoServicio().eliminar(id(105));
        comparar("Administrador elimina equipo fuera de almacen y sin historial", false,
                contieneEquipo(contexto.equipoServicio().listar(), id(105)));
        esperar("No elimina equipo presente en almacen", IllegalStateException.class,
                () -> contexto.equipoServicio().eliminar(id(101)));

        contexto.reloj().fijar(fecha(1, 10, 10));
        contexto.movimientoServicio().cambiarEstado(id(102), EstadoEquipo.DISPONIBLE, "Mantenimiento concluido");
        contexto.movimientoServicio().registrarSalida(id(102), "Sede externa", "Retiro para eliminacion");
        esperar("No elimina equipo fuera del almacen con historial", IllegalStateException.class,
                () -> contexto.equipoServicio().eliminar(id(102)));

        esperar("No anula equipo con movimientos posteriores", IllegalStateException.class,
                () -> contexto.equipoServicio().anularAlta(id(101), "Alta equivocada"));
        contexto.equipoServicio().registrarEquipo(id(109), "EQ-109", "SER-109", "HP", "ProBook", id(1), id(11));
        esperar("No anula equipo sin ingreso inicial", IllegalStateException.class,
                () -> contexto.equipoServicio().anularAlta(id(109), "Alta equivocada"));

        contexto.reloj().fijar(fecha(1, 10, 11));
        int stockAntesAnulacion = contexto.stockServicio()
                .consultarDisponibilidad(id(1), "Lima actualizada").totalPresente();
        contexto.equipoServicio().registrarEquipoConIngreso(id(108), "EQ-108", "SER-108", "HP", "ProBook",
                id(1), id(11), id(21), "Ingreso inicial");
        comparar("Ingreso inicial aumenta el stock antes de anular", stockAntesAnulacion + 1,
                contexto.stockServicio().consultarDisponibilidad(id(1), "Lima actualizada").totalPresente());
        ResultadoMovimiento anulacion = contexto.equipoServicio().anularAlta(id(108), "Equipo duplicado");
        comparar("Anulacion registra un movimiento especifico", TipoMovimiento.ANULACION_ALTA,
                anulacion.movimiento().getTipo());
        comparar("Anulacion conserva el motivo", "Equipo duplicado", anulacion.movimiento().getMotivo());
        comparar("Anulacion deja equipo terminal y fuera de almacen", true,
                contexto.equipoServicio().consultarPorId(id(108)).estado() == EstadoEquipo.ANULADO
                        && !contexto.equipoServicio().consultarPorId(id(108)).presenteEnAlmacen());
        comparar("Anulacion restaura el conteo de stock", stockAntesAnulacion,
                contexto.stockServicio().consultarDisponibilidad(id(1), "Lima actualizada").totalPresente());
        comparar("Anulacion conserva ingreso y evento de auditoria", List.of(TipoMovimiento.INGRESO,
                        TipoMovimiento.ANULACION_ALTA), contexto.movimientoServicio().historialPorEquipo(id(108))
                        .stream().map(Movimiento::getTipo).toList());
        comparar("Equipo anulado se filtra del listado operativo", false,
                contieneEquipo(contexto.equipoServicio().listar(), id(108)));
        esperar("No se puede reingresar un equipo anulado", IllegalStateException.class,
                () -> contexto.movimientoServicio().registrarIngreso(id(108), id(21), "Intento de reingreso"));
        comparar("Reingreso bloqueado no agrega historial", 2,
                contexto.movimientoServicio().historialPorEquipo(id(108)).size());
    }

    private static void verificarCompensacion(Contexto contexto) {
        EquipoConsulta equipoPrueba = contexto.equipoServicio().registrarEquipo(
                id(106), "EQ-106", "SER-106", "Asus", "ExpertBook", id(1), id(11));
        int totalMovimientosAntes = contexto.movimientoDAO().listar().size();
        MovimientoDAO daoFallido = new MovimientoDAOConFalloDeInsercion(contexto.movimientoDAO());
        MovimientoServicio servicioFallido = new MovimientoServicio(contexto.equipoDAO(), daoFallido,
                contexto.categoriaDAO(), contexto.proveedorDAO(), contexto.ubicacionDAO(), contexto.sesion(),
                contexto.autorizacion(), contexto.stockServicio(), contexto.reloj());
        EquipoServicio equiposConServicioFallido = new EquipoServicio(contexto.equipoDAO(), contexto.categoriaDAO(),
                contexto.proveedorDAO(), contexto.ubicacionDAO(), daoFallido, servicioFallido,
                contexto.sesion(), contexto.autorizacion());

        esperar("Fallo de movimiento informa error", IllegalStateException.class,
                () -> servicioFallido.registrarIngreso(id(106), id(21), "Ingreso que falla"));
        EquipoConsulta restaurado = contexto.equipoServicio().consultarPorId(id(106));
        comparar("Fallo de historial restaura el equipo", true,
                restaurado.equals(equipoPrueba) && !restaurado.presenteEnAlmacen());
        comparar("Fallo de historial no deja evento parcial", totalMovimientosAntes,
                contexto.movimientoDAO().listar().size());

        esperar("Fallo de ingreso inicial informa error", IllegalStateException.class,
                () -> equiposConServicioFallido.registrarEquipoConIngreso(id(107), "EQ-107", "SER-107",
                        "Acer", "Swift", id(1), id(11), id(21), "Alta que falla"));
        comparar("Fallo de ingreso inicial elimina el alta parcial", false,
                contieneEquipo(contexto.equipoServicio().listar(), id(107)));
        comparar("Fallo de ingreso inicial conserva historial", totalMovimientosAntes,
                contexto.movimientoDAO().listar().size());

        contexto.reloj().fijar(fecha(1, 10, 11));
        contexto.movimientoServicio().registrarIngreso(id(106), id(21), "Ingreso para probar anulacion");
        EquipoConsulta antesDeAnular = contexto.equipoServicio().consultarPorId(id(106));
        int movimientosAntesDeAnular = contexto.movimientoDAO().listar().size();
        esperar("Fallo de anulacion informa error", IllegalStateException.class,
                () -> servicioFallido.anularAlta(id(106), "Anulacion que falla"));
        comparar("Fallo al guardar anulacion restaura equipo", antesDeAnular,
                contexto.equipoServicio().consultarPorId(id(106)));
        comparar("Fallo al guardar anulacion conserva ingreso y evita evento parcial",
                movimientosAntesDeAnular, contexto.movimientoDAO().listar().size());
    }

    private static Contexto crearContexto() {
        EquipoDAO equipoDAO = new EquipoDAOMemoria();
        MovimientoDAO movimientoDAO = new MovimientoDAOMemoria();
        ConfiguracionStockMinimoDAO configuracionDAO = new ConfiguracionStockMinimoDAOMemoria();
        CategoriaDAO categoriaDAO = new CategoriaDAOMemoria();
        ProveedorDAO proveedorDAO = new ProveedorDAOMemoria();
        UbicacionDAO ubicacionDAO = new UbicacionDAOMemoria();
        UsuarioDAO usuarioDAO = new UsuarioDAOMemoria();

        categoriaDAO.insertar(new Categoria(id(1), "Laptops", "Equipos portatiles"));
        proveedorDAO.insertar(new Proveedor(id(11), "Proveedor Uno", "999111222", "contacto@uno.pe"));
        ubicacionDAO.insertar(new Ubicacion(id(21), "Lima", "Almacen Lima A", "Inventario", "PB", true));
        ubicacionDAO.insertar(new Ubicacion(id(22), "Lima", "Almacen Lima B", "Inventario", "Sotano", true));
        ubicacionDAO.insertar(new Ubicacion(id(23), "Lima", "Operaciones", "Soporte", "2", false));
        ubicacionDAO.insertar(new Ubicacion(id(24), "Arequipa", "Almacen Arequipa", "Inventario", "PB", true));

        GestorContrasenas gestor = new GestorContrasenas();
        insertarUsuario(usuarioDAO, gestor, id(31), "admin", Rol.ADMINISTRADOR, contrasenaAdmin());
        insertarUsuario(usuarioDAO, gestor, id(32), "almacen", Rol.ALMACENERO, contrasenaAlmacenero());
        insertarUsuario(usuarioDAO, gestor, id(33), "auditor", Rol.AUDITOR, contrasenaAuditor());

        Sesion sesion = new Sesion();
        AutenticacionServicio autenticacion = new AutenticacionServicio(usuarioDAO, gestor, sesion);
        AutorizacionServicio autorizacion = new AutorizacionServicio(sesion);
        StockServicio stockServicio = new StockServicio(categoriaDAO, configuracionDAO, equipoDAO,
                ubicacionDAO, sesion, autorizacion);
        RelojControlado reloj = new RelojControlado(fecha(1, 10, 0), ZoneId.of("UTC"));
        MovimientoServicio movimientoServicio = new MovimientoServicio(equipoDAO, movimientoDAO, categoriaDAO,
                proveedorDAO, ubicacionDAO, sesion, autorizacion, stockServicio, reloj);
        EquipoServicio equipoServicio = new EquipoServicio(equipoDAO, categoriaDAO, proveedorDAO, ubicacionDAO,
                movimientoDAO, movimientoServicio, sesion, autorizacion);
        return new Contexto(equipoDAO, movimientoDAO, categoriaDAO, configuracionDAO, proveedorDAO, ubicacionDAO,
                sesion, autenticacion, autorizacion, stockServicio, reloj, equipoServicio, movimientoServicio);
    }

    private static void insertarUsuario(UsuarioDAO usuarioDAO, GestorContrasenas gestor, UUID id,
                                        String nombre, Rol rol, char[] contrasena) {
        GestorContrasenas.Credencial credencial = gestor.generarHash(contrasena);
        usuarioDAO.insertar(new Usuario(id, nombre, credencial.getHashContrasena(),
                credencial.getSalContrasena(), rol));
    }

    private static boolean contieneEquipo(List<EquipoConsulta> equipos, UUID id) {
        return equipos.stream().anyMatch(equipo -> equipo.id().equals(id));
    }

    private static LocalDateTime fecha(int dia, int hora, int minuto) {
        return LocalDateTime.of(2026, 8, dia, hora, minuto);
    }

    private static UUID id(long valor) {
        return new UUID(0L, valor);
    }

    private static char[] contrasenaAdmin() {
        return "demo-admin-inventario".toCharArray();
    }

    private static char[] contrasenaAlmacenero() {
        return "demo-almacen-inventario".toCharArray();
    }

    private static char[] contrasenaAuditor() {
        return "demo-auditor-inventario".toCharArray();
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

    private record Contexto(
            EquipoDAO equipoDAO,
            MovimientoDAO movimientoDAO,
            CategoriaDAO categoriaDAO,
            ConfiguracionStockMinimoDAO configuracionDAO,
            ProveedorDAO proveedorDAO,
            UbicacionDAO ubicacionDAO,
            Sesion sesion,
            AutenticacionServicio autenticacion,
            AutorizacionServicio autorizacion,
            StockServicio stockServicio,
            RelojControlado reloj,
            EquipoServicio equipoServicio,
            MovimientoServicio movimientoServicio) {
    }

    private static final class RelojControlado extends Clock {
        private final ZoneId zona;
        private Instant instante;

        private RelojControlado(LocalDateTime fechaInicial, ZoneId zona) {
            this.zona = zona;
            fijar(fechaInicial);
        }

        private RelojControlado(Instant instante, ZoneId zona) {
            this.zona = zona;
            this.instante = instante;
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
            return new RelojControlado(instante, zonaNueva);
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    private static final class MovimientoDAOConFalloDeInsercion implements MovimientoDAO {
        private final MovimientoDAO delegado;

        private MovimientoDAOConFalloDeInsercion(MovimientoDAO delegado) {
            this.delegado = delegado;
        }

        @Override
        public void insertar(Movimiento movimiento) {
            throw new IllegalStateException("Fallo deliberado de insercion para verificar compensacion");
        }

        @Override
        public Optional<Movimiento> buscarPorId(UUID id) {
            return delegado.buscarPorId(id);
        }

        @Override
        public List<Movimiento> listar() {
            return delegado.listar();
        }

        @Override
        public List<Movimiento> listarPorEquipo(UUID equipoId) {
            return delegado.listarPorEquipo(equipoId);
        }

        @Override
        public List<Movimiento> listarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
            return delegado.listarPorRangoFechas(inicio, fin);
        }
    }
}