package com.computototal.inventario.servicio;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.computototal.inventario.dao.CategoriaDAO;
import com.computototal.inventario.dao.EquipoDAO;
import com.computototal.inventario.dao.MovimientoDAO;
import com.computototal.inventario.dao.ProveedorDAO;
import com.computototal.inventario.dao.UbicacionDAO;
import com.computototal.inventario.modelo.Categoria;
import com.computototal.inventario.modelo.Equipo;
import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.Proveedor;
import com.computototal.inventario.modelo.Ubicacion;

public final class EquipoServicio {
    private final EquipoDAO equipoDAO;
    private final CategoriaDAO categoriaDAO;
    private final ProveedorDAO proveedorDAO;
    private final UbicacionDAO ubicacionDAO;
    private final MovimientoDAO movimientoDAO;
    private final MovimientoServicio movimientoServicio;
    private final Sesion sesion;
    private final AutorizacionServicio autorizacionServicio;

    public EquipoServicio(EquipoDAO equipoDAO, CategoriaDAO categoriaDAO, ProveedorDAO proveedorDAO,
                          UbicacionDAO ubicacionDAO, MovimientoDAO movimientoDAO,
                          MovimientoServicio movimientoServicio, Sesion sesion,
                          AutorizacionServicio autorizacionServicio) {
        this.equipoDAO = Objects.requireNonNull(equipoDAO, "equipoDAO");
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO");
        this.proveedorDAO = Objects.requireNonNull(proveedorDAO, "proveedorDAO");
        this.ubicacionDAO = Objects.requireNonNull(ubicacionDAO, "ubicacionDAO");
        this.movimientoDAO = Objects.requireNonNull(movimientoDAO, "movimientoDAO");
        this.movimientoServicio = Objects.requireNonNull(movimientoServicio, "movimientoServicio");
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.autorizacionServicio = Objects.requireNonNull(autorizacionServicio, "autorizacionServicio");
    }

    public EquipoConsulta registrarEquipo(UUID id, String codigo, String numeroSerie,
                                          String marca, String modelo, UUID categoriaId, UUID proveedorId) {
        return registrarEquipoInterno(id, codigo, numeroSerie, marca, modelo, categoriaId, proveedorId,
            Optional.empty(), null).equipo();
    }

        public ResultadoAltaEquipo registrarEquipoConIngreso(UUID id, String codigo, String numeroSerie,
                                 String marca, String modelo, UUID categoriaId,
                                 UUID proveedorId, UUID ubicacionAlmacenId, String motivo) {
        return registrarEquipoInterno(id, codigo, numeroSerie, marca, modelo, categoriaId, proveedorId,
                Optional.of(Objects.requireNonNull(ubicacionAlmacenId, "ubicacionAlmacenId")), motivo);
    }

    public EquipoConsulta consultarPorCodigo(String codigo) {
        autorizar(Permiso.CONSULTAR_INVENTARIO);
        Equipo equipo = equipoDAO.buscarPorCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("No existe equipo con codigo " + codigo));
        return crearConsulta(equipo);
    }

    public EquipoConsulta consultarPorNumeroSerie(String numeroSerie) {
        autorizar(Permiso.CONSULTAR_INVENTARIO);
        Equipo equipo = equipoDAO.buscarPorNumeroSerie(numeroSerie)
                .orElseThrow(() -> new NoSuchElementException("No existe equipo con numero de serie " + numeroSerie));
        return crearConsulta(equipo);
    }

    public EquipoConsulta consultarPorId(UUID id) {
        autorizar(Permiso.CONSULTAR_INVENTARIO);
        return crearConsulta(buscarEquipo(id));
    }

    public List<EquipoConsulta> listar() {
        autorizar(Permiso.CONSULTAR_INVENTARIO);
        return equipoDAO.listar().stream().map(this::crearConsulta).toList();
    }

    public List<ReferenciaCatalogo> listarCategorias() {
        autorizar(Permiso.CONSULTAR_INVENTARIO);
        return categoriaDAO.listar().stream()
                .map(categoria -> new ReferenciaCatalogo(categoria.getId(), categoria.getNombre()))
                .toList();
    }

    public List<ReferenciaCatalogo> listarProveedores() {
        autorizar(Permiso.CONSULTAR_INVENTARIO);
        return proveedorDAO.listar().stream()
                .map(proveedor -> new ReferenciaCatalogo(proveedor.getId(), proveedor.getNombre()))
                .toList();
    }

    public List<EquipoConsulta.UbicacionResumen> listarUbicaciones() {
        autorizar(Permiso.CONSULTAR_INVENTARIO);
        return ubicacionDAO.listar().stream()
                .map(ubicacion -> new EquipoConsulta.UbicacionResumen(ubicacion.getId(), ubicacion.getSede(),
                        ubicacion.getAmbiente(), ubicacion.getArea(), ubicacion.getPiso(), ubicacion.isAlmacen()))
                .toList();
    }

    public void eliminar(UUID equipoId) {
        autorizar(Permiso.ELIMINAR_REGISTROS);
        Equipo equipo = buscarEquipo(equipoId);
        if (equipo.isPresenteEnAlmacen()) {
            throw new IllegalStateException("Solo se puede eliminar un equipo fuera del almacen");
        }
        if (!movimientoDAO.listarPorEquipo(equipo.getId()).isEmpty()) {
            throw new IllegalStateException("No se puede eliminar un equipo con historial");
        }
        equipoDAO.eliminar(equipo.getId());
    }

    private ResultadoAltaEquipo registrarEquipoInterno(UUID id, String codigo, String numeroSerie,
                                                       String marca, String modelo, UUID categoriaId,
                                                       UUID proveedorId, Optional<UUID> ubicacionInicialId,
                                                       String motivo) {
        autorizar(Permiso.REGISTRAR_EQUIPOS);
        Objects.requireNonNull(ubicacionInicialId, "ubicacionInicialId");
        if (ubicacionInicialId.isPresent()) {
            autorizar(Permiso.REGISTRAR_INGRESOS);
            validarTexto(motivo, "motivo");
        }

        Categoria categoria = categoriaDAO.buscarPorId(Objects.requireNonNull(categoriaId, "categoriaId"))
                .orElseThrow(() -> new NoSuchElementException("No existe categoria con identificador " + categoriaId));
        Proveedor proveedor = proveedorDAO.buscarPorId(Objects.requireNonNull(proveedorId, "proveedorId"))
                .orElseThrow(() -> new NoSuchElementException("No existe proveedor con identificador " + proveedorId));
        Ubicacion ubicacionInicial = ubicacionInicialId
                .map(this::buscarUbicacion)
                .orElse(null);
        if (ubicacionInicial != null && !ubicacionInicial.isAlmacen()) {
            throw new IllegalArgumentException("El alta con ingreso requiere una ubicacion de almacen");
        }

        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(numeroSerie, "numeroSerie");
        if (equipoDAO.buscarPorCodigo(codigo).isPresent()) {
            throw new IllegalArgumentException("Ya existe un equipo con ese codigo");
        }
        if (equipoDAO.buscarPorNumeroSerie(numeroSerie).isPresent()) {
            throw new IllegalArgumentException("Ya existe un equipo con ese numero de serie");
        }

        Equipo equipo = new Equipo(Objects.requireNonNull(id, "id"), codigo, numeroSerie, marca, modelo,
                categoria.getId(), proveedor.getId(), EstadoEquipo.DISPONIBLE);
        equipoDAO.insertar(equipo);
        ResultadoMovimiento resultadoIngreso = null;
        if (ubicacionInicial != null) {
            try {
            resultadoIngreso = movimientoServicio.registrarIngreso(equipo.getId(), ubicacionInicial.getId(), motivo);
            } catch (RuntimeException falloIngreso) {
                try {
                    equipoDAO.eliminar(equipo.getId());
                } catch (RuntimeException falloCompensacion) {
                    IllegalStateException falloCompuesto = new IllegalStateException(
                            "Fallo el ingreso inicial y tambien la eliminacion compensatoria del alta", falloIngreso);
                    falloCompuesto.addSuppressed(falloCompensacion);
                    throw falloCompuesto;
                }
                throw falloIngreso;
            }
        }
        return new ResultadoAltaEquipo(crearConsulta(buscarEquipo(equipo.getId())),
                Optional.ofNullable(resultadoIngreso));
    }

    private EquipoConsulta crearConsulta(Equipo equipo) {
        Categoria categoria = categoriaDAO.buscarPorId(equipo.getCategoriaId())
                .orElseThrow(() -> new IllegalStateException("No existe la categoria referenciada por el equipo"));
        Proveedor proveedor = proveedorDAO.buscarPorId(equipo.getProveedorId())
                .orElseThrow(() -> new IllegalStateException("No existe el proveedor referenciado por el equipo"));
        return new EquipoConsulta(equipo.getId(), equipo.getCodigo(), equipo.getNumeroSerie(), equipo.getMarca(),
                equipo.getModelo(), categoria.getId(), categoria.getNombre(), proveedor.getId(), proveedor.getNombre(),
                equipo.getEstado(), equipo.isPresenteEnAlmacen(), resumirUbicacion(equipo.getUbicacionActualId()),
                resumirUbicacion(equipo.getUltimaUbicacionAlmacenId()), equipo.getDestinoSalida());
    }

    private Optional<EquipoConsulta.UbicacionResumen> resumirUbicacion(Optional<UUID> ubicacionId) {
        return ubicacionId.map(this::buscarUbicacion).map(ubicacion -> new EquipoConsulta.UbicacionResumen(
                ubicacion.getId(), ubicacion.getSede(), ubicacion.getAmbiente(), ubicacion.getArea(),
                ubicacion.getPiso(), ubicacion.isAlmacen()));
    }

    private Equipo buscarEquipo(UUID id) {
        Objects.requireNonNull(id, "equipoId");
        return equipoDAO.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe equipo con identificador " + id));
    }

    private Ubicacion buscarUbicacion(UUID id) {
        return ubicacionDAO.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe ubicacion con identificador " + id));
    }

    private void autorizar(Permiso permiso) {
        autorizacionServicio.exigirPermiso(permiso);
        sesion.exigirSesionActiva();
    }

    private String validarTexto(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " no puede ser null");
        String normalizado = valor.strip();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException(campo + " no puede estar vacio");
        }
        return normalizado;
    }
}