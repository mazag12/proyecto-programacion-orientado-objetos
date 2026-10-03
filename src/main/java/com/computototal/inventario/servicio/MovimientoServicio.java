package com.computototal.inventario.servicio;

import com.computototal.inventario.dao.CategoriaDAO;
import com.computototal.inventario.dao.EquipoDAO;
import com.computototal.inventario.dao.MovimientoDAO;
import com.computototal.inventario.dao.ProveedorDAO;
import com.computototal.inventario.dao.UbicacionDAO;
import com.computototal.inventario.modelo.Categoria;
import com.computototal.inventario.modelo.Equipo;
import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.Movimiento;
import com.computototal.inventario.modelo.Proveedor;
import com.computototal.inventario.modelo.TipoMovimiento;
import com.computototal.inventario.modelo.Ubicacion;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MovimientoServicio {
    private final EquipoDAO equipoDAO;
    private final MovimientoDAO movimientoDAO;
    private final CategoriaDAO categoriaDAO;
    private final ProveedorDAO proveedorDAO;
    private final UbicacionDAO ubicacionDAO;
    private final Sesion sesion;
    private final AutorizacionServicio autorizacionServicio;
    private final StockServicio stockServicio;
    private final Clock reloj;

    public MovimientoServicio(EquipoDAO equipoDAO, MovimientoDAO movimientoDAO,
                              CategoriaDAO categoriaDAO, ProveedorDAO proveedorDAO,
                              UbicacionDAO ubicacionDAO, Sesion sesion,
                              AutorizacionServicio autorizacionServicio, StockServicio stockServicio, Clock reloj) {
        this.equipoDAO = Objects.requireNonNull(equipoDAO, "equipoDAO");
        this.movimientoDAO = Objects.requireNonNull(movimientoDAO, "movimientoDAO");
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO");
        this.proveedorDAO = Objects.requireNonNull(proveedorDAO, "proveedorDAO");
        this.ubicacionDAO = Objects.requireNonNull(ubicacionDAO, "ubicacionDAO");
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.autorizacionServicio = Objects.requireNonNull(autorizacionServicio, "autorizacionServicio");
        this.stockServicio = Objects.requireNonNull(stockServicio, "stockServicio");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    public ResultadoMovimiento registrarIngreso(UUID equipoId, UUID ubicacionId, String motivo) {
        SesionUsuario responsable = autorizar(Permiso.REGISTRAR_INGRESOS);
        String motivoValidado = validarTexto(motivo, "motivo");
        Equipo original = buscarEquipo(equipoId);
        Ubicacion destino = buscarUbicacion(ubicacionId);
        if (!destino.isAlmacen()) {
            throw new IllegalArgumentException("El ingreso requiere una ubicacion de almacen");
        }

        Equipo actualizado = original.copiar();
        actualizado.registrarIngreso(destino);
        LocalDateTime fechaHora = validarFechaSiguiente(original.getId());
        Movimiento movimiento = crearMovimiento(fechaHora, TipoMovimiento.INGRESO, motivoValidado,
                actualizado, responsable, Optional.empty(), Optional.of(destino), Optional.empty(),
                Optional.of(original.getEstado()), Optional.of(actualizado.getEstado()),
                original.isPresenteEnAlmacen(), actualizado.isPresenteEnAlmacen());
        persistirMovimiento(original, actualizado, movimiento);
        return resultado(movimiento, original.getCategoriaId(), destino.getSede());
    }

    public ResultadoMovimiento registrarSalida(UUID equipoId, String destinoExterno, String motivo) {
        SesionUsuario responsable = autorizar(Permiso.REGISTRAR_SALIDAS);
        String motivoValidado = validarTexto(motivo, "motivo");
        String destinoValidado = validarTexto(destinoExterno, "destinoExterno");
        Equipo original = buscarEquipo(equipoId);
        exigirPresenteYDisponible(original, "salir");
        Ubicacion origen = buscarUbicacion(original.getUbicacionActualId()
                .orElseThrow(() -> new IllegalStateException("El equipo no tiene ubicacion actual")));

        Equipo actualizado = original.copiar();
        actualizado.registrarSalida(destinoValidado);
        LocalDateTime fechaHora = validarFechaSiguiente(original.getId());
        Movimiento movimiento = crearMovimiento(fechaHora, TipoMovimiento.SALIDA, motivoValidado,
                actualizado, responsable, Optional.of(origen), Optional.empty(), Optional.of(destinoValidado),
                Optional.of(original.getEstado()), Optional.of(actualizado.getEstado()),
                original.isPresenteEnAlmacen(), actualizado.isPresenteEnAlmacen());
        persistirMovimiento(original, actualizado, movimiento);
        return resultado(movimiento, original.getCategoriaId(), origen.getSede());
    }

    public ResultadoMovimiento trasladar(UUID equipoId, UUID ubicacionDestinoId, String motivo) {
        SesionUsuario responsable = autorizar(Permiso.REGISTRAR_TRASLADOS);
        String motivoValidado = validarTexto(motivo, "motivo");
        Equipo original = buscarEquipo(equipoId);
        exigirPresenteYDisponible(original, "trasladar");
        Ubicacion origen = buscarUbicacion(original.getUbicacionActualId()
                .orElseThrow(() -> new IllegalStateException("El equipo no tiene ubicacion actual")));
        Ubicacion destino = buscarUbicacion(ubicacionDestinoId);
        if (!destino.isAlmacen()) {
            throw new IllegalArgumentException("El traslado requiere un destino de almacen");
        }
        if (mismaSede(origen.getSede(), destino.getSede())) {
            throw new IllegalArgumentException("El traslado requiere una sede distinta; use cambio de ubicacion");
        }

        Equipo actualizado = original.copiar();
        actualizado.trasladarAAlmacen(destino);
        LocalDateTime fechaHora = validarFechaSiguiente(original.getId());
        Movimiento movimiento = crearMovimiento(fechaHora, TipoMovimiento.TRASLADO, motivoValidado,
                actualizado, responsable, Optional.of(origen), Optional.of(destino), Optional.empty(),
                Optional.of(original.getEstado()), Optional.of(actualizado.getEstado()),
                original.isPresenteEnAlmacen(), actualizado.isPresenteEnAlmacen());
        persistirMovimiento(original, actualizado, movimiento);
        return resultado(movimiento, original.getCategoriaId(), origen.getSede(), destino.getSede());
    }

    public ResultadoMovimiento cambiarUbicacion(UUID equipoId, UUID ubicacionDestinoId, String motivo) {
        SesionUsuario responsable = autorizar(Permiso.CAMBIAR_UBICACION_EQUIPO);
        String motivoValidado = validarTexto(motivo, "motivo");
        Equipo original = buscarEquipo(equipoId);
        if (original.getEstado() == EstadoEquipo.DE_BAJA) {
            throw new IllegalStateException("Un equipo dado de baja no puede cambiar de ubicacion");
        }
        if (!original.isPresenteEnAlmacen()) {
            throw new IllegalStateException("Solo puede cambiarse la ubicacion de un equipo presente en almacen");
        }
        Ubicacion origen = buscarUbicacion(original.getUbicacionActualId()
                .orElseThrow(() -> new IllegalStateException("El equipo no tiene ubicacion actual")));
        Ubicacion destino = buscarUbicacion(ubicacionDestinoId);
        if (origen.getId().equals(destino.getId())) {
            throw new IllegalArgumentException("El equipo ya se encuentra en esa ubicacion");
        }
        if (!mismaSede(origen.getSede(), destino.getSede())) {
            throw new IllegalArgumentException("Para cambiar de sede debe registrar un traslado");
        }

        Equipo actualizado = original.copiar();
        actualizado.cambiarUbicacion(destino);
        LocalDateTime fechaHora = validarFechaSiguiente(original.getId());
        Movimiento movimiento = crearMovimiento(fechaHora, TipoMovimiento.CAMBIO_UBICACION, motivoValidado,
                actualizado, responsable, Optional.of(origen), Optional.of(destino), Optional.empty(),
                Optional.of(original.getEstado()), Optional.of(actualizado.getEstado()),
                original.isPresenteEnAlmacen(), actualizado.isPresenteEnAlmacen());
        persistirMovimiento(original, actualizado, movimiento);
        return resultado(movimiento, original.getCategoriaId(), origen.getSede(), destino.getSede());
    }

    public ResultadoMovimiento cambiarEstado(UUID equipoId, EstadoEquipo nuevoEstado, String justificacion) {
        SesionUsuario responsable = autorizar(Permiso.CAMBIAR_ESTADO_EQUIPO);
        String motivoValidado = validarTexto(justificacion, "justificacion");
        Equipo original = buscarEquipo(equipoId);
        Equipo actualizado = original.copiar();
        actualizado.cambiarEstado(nuevoEstado);
        Optional<Ubicacion> ubicacionActual = original.getUbicacionActualId().map(this::buscarUbicacion);
        LocalDateTime fechaHora = validarFechaSiguiente(original.getId());
        Movimiento movimiento = crearMovimiento(fechaHora, TipoMovimiento.CAMBIO_ESTADO, motivoValidado,
            actualizado, responsable, ubicacionActual, ubicacionActual, Optional.empty(),
                Optional.of(original.getEstado()), Optional.of(actualizado.getEstado()),
                original.isPresenteEnAlmacen(), actualizado.isPresenteEnAlmacen());
        persistirMovimiento(original, actualizado, movimiento);
        if (original.isPresenteEnAlmacen() && ubicacionActual.isPresent()) {
            return resultado(movimiento, original.getCategoriaId(), ubicacionActual.get().getSede());
        }
        return new ResultadoMovimiento(movimiento, List.of());
    }

    public ResultadoMovimiento anularAlta(UUID equipoId, String motivo) {
        SesionUsuario responsable = autorizar(Permiso.ANULAR_ALTAS);
        String motivoValidado = validarTexto(motivo, "motivo");
        Equipo original = buscarEquipo(equipoId);
        List<Movimiento> historial = movimientoDAO.listarPorEquipo(equipoId);
        if (historial.size() != 1 || historial.get(0).getTipo() != TipoMovimiento.INGRESO) {
            throw new IllegalStateException("Solo se puede anular un alta con un unico ingreso inicial");
        }
        Ubicacion origen = buscarUbicacion(original.getUbicacionActualId()
                .orElseThrow(() -> new IllegalStateException("El equipo no tiene ubicacion actual")));
        if (!origen.isAlmacen()) {
            throw new IllegalStateException("Solo se puede anular un alta mientras el equipo siga en almacen");
        }

        Equipo actualizado = original.copiar();
        actualizado.anularAlta();
        LocalDateTime fechaHora = validarFechaSiguiente(original.getId());
        Movimiento movimiento = crearMovimiento(fechaHora, TipoMovimiento.ANULACION_ALTA, motivoValidado,
                actualizado, responsable, Optional.of(origen), Optional.empty(), Optional.empty(),
                Optional.of(original.getEstado()), Optional.of(actualizado.getEstado()),
                original.isPresenteEnAlmacen(), actualizado.isPresenteEnAlmacen());
        persistirMovimiento(original, actualizado, movimiento);
        return resultado(movimiento, original.getCategoriaId(), origen.getSede());
    }

    public List<Movimiento> historialPorEquipo(UUID equipoId) {
        autorizar(Permiso.CONSULTAR_HISTORIAL);
        Equipo equipo = buscarEquipo(equipoId);
        return List.copyOf(movimientoDAO.listarPorEquipo(equipo.getId()));
    }

    public List<Movimiento> historialPorCodigo(String codigo) {
        autorizar(Permiso.CONSULTAR_HISTORIAL);
        Equipo equipo = equipoDAO.buscarPorCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("No existe equipo con codigo " + codigo));
        return List.copyOf(movimientoDAO.listarPorEquipo(equipo.getId()));
    }

    public List<Movimiento> historialPorNumeroSerie(String numeroSerie) {
        autorizar(Permiso.CONSULTAR_HISTORIAL);
        Equipo equipo = equipoDAO.buscarPorNumeroSerie(numeroSerie)
                .orElseThrow(() -> new NoSuchElementException("No existe equipo con numero de serie " + numeroSerie));
        return List.copyOf(movimientoDAO.listarPorEquipo(equipo.getId()));
    }

    private SesionUsuario autorizar(Permiso permiso) {
        autorizacionServicio.exigirPermiso(permiso);
        return sesion.exigirSesionActiva();
    }

    private Equipo buscarEquipo(UUID id) {
        Objects.requireNonNull(id, "equipoId");
        return equipoDAO.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe equipo con identificador " + id));
    }

    private Ubicacion buscarUbicacion(UUID id) {
        Objects.requireNonNull(id, "ubicacionId");
        return ubicacionDAO.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe ubicacion con identificador " + id));
    }

    private void exigirPresenteYDisponible(Equipo equipo, String operacion) {
        if (equipo.getEstado() == EstadoEquipo.DE_BAJA) {
            throw new IllegalStateException("Un equipo dado de baja no puede " + operacion);
        }
        if (!equipo.isPresenteEnAlmacen()) {
            throw new IllegalStateException("Solo puede " + operacion + " un equipo presente en almacen");
        }
        if (equipo.getEstado() != EstadoEquipo.DISPONIBLE) {
            throw new IllegalStateException("Solo puede " + operacion + " un equipo disponible");
        }
    }

    private Movimiento crearMovimiento(LocalDateTime fechaHora, TipoMovimiento tipo, String motivo,
                                       Equipo equipo, SesionUsuario responsable,
                                       Optional<Ubicacion> origen, Optional<Ubicacion> destino,
                                       Optional<String> destinoExterno, Optional<EstadoEquipo> estadoAnterior,
                                       Optional<EstadoEquipo> estadoNuevo, boolean presenciaAnterior,
                                       boolean presenciaNueva) {
        Categoria categoria = categoriaDAO.buscarPorId(equipo.getCategoriaId())
                .orElseThrow(() -> new NoSuchElementException("No existe la categoria del equipo"));
        Proveedor proveedor = proveedorDAO.buscarPorId(equipo.getProveedorId())
                .orElseThrow(() -> new NoSuchElementException("No existe el proveedor del equipo"));
        Movimiento.InstantaneaEquipo instantaneaEquipo = new Movimiento.InstantaneaEquipo(
            equipo.getId(), equipo.getCategoriaId(), equipo.getCodigo(), equipo.getNumeroSerie(),
            equipo.getMarca(), equipo.getModelo(),
                categoria.getNombre(), proveedor.getNombre());
        Optional<Movimiento.InstantaneaUbicacion> origenInstantaneo = origen.map(this::crearInstantanea);
        Optional<Movimiento.InstantaneaUbicacion> destinoInstantaneo = destino.map(this::crearInstantanea);
        return new Movimiento(UUID.randomUUID(), fechaHora, tipo, motivo, instantaneaEquipo,
                responsable.id(), responsable.nombreUsuario(), origenInstantaneo, destinoInstantaneo,
                destinoExterno, estadoAnterior, estadoNuevo, presenciaAnterior, presenciaNueva);
    }

    private ResultadoMovimiento resultado(Movimiento movimiento, UUID categoriaId, String... sedes) {
        return new ResultadoMovimiento(movimiento,
                stockServicio.evaluarAfectadas(categoriaId, List.of(sedes)));
    }

    private Movimiento.InstantaneaUbicacion crearInstantanea(Ubicacion ubicacion) {
        return new Movimiento.InstantaneaUbicacion(ubicacion.getId(), ubicacion.getSede(), ubicacion.getAmbiente(),
                ubicacion.getArea(), ubicacion.getPiso(), ubicacion.isAlmacen());
    }

    private LocalDateTime validarFechaSiguiente(UUID equipoId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Movimiento> historial = movimientoDAO.listarPorEquipo(equipoId);
        if (!historial.isEmpty() && ahora.isBefore(historial.get(historial.size() - 1).getFechaHora())) {
            throw new IllegalArgumentException("La fecha del movimiento no puede ser anterior al ultimo movimiento");
        }
        return ahora;
    }

    private void persistirMovimiento(Equipo original, Equipo actualizado, Movimiento movimiento) {
        equipoDAO.actualizar(actualizado);
        try {
            movimientoDAO.insertar(movimiento);
        } catch (RuntimeException falloMovimiento) {
            try {
                equipoDAO.actualizar(original);
            } catch (RuntimeException falloCompensacion) {
                IllegalStateException falloCompuesto = new IllegalStateException(
                        "Fallo el movimiento y tambien la restauracion del equipo", falloMovimiento);
                falloCompuesto.addSuppressed(falloCompensacion);
                throw falloCompuesto;
            }
            throw new IllegalStateException("No se pudo guardar el movimiento; el equipo fue restaurado", falloMovimiento);
        }
    }

    private String validarTexto(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " no puede ser null");
        String normalizado = valor.strip();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException(campo + " no puede estar vacio");
        }
        return normalizado;
    }

    private boolean mismaSede(String sedeUno, String sedeDos) {
        return sedeUno.strip().equalsIgnoreCase(sedeDos.strip());
    }
}