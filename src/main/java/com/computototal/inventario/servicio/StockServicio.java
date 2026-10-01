package com.computototal.inventario.servicio;

import com.computototal.inventario.dao.CategoriaDAO;
import com.computototal.inventario.dao.ConfiguracionStockMinimoDAO;
import com.computototal.inventario.dao.EquipoDAO;
import com.computototal.inventario.dao.UbicacionDAO;
import com.computototal.inventario.modelo.Categoria;
import com.computototal.inventario.modelo.ConfiguracionStockMinimo;
import com.computototal.inventario.modelo.Equipo;
import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.Ubicacion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class StockServicio {
    private final CategoriaDAO categoriaDAO;
    private final ConfiguracionStockMinimoDAO configuracionDAO;
    private final EquipoDAO equipoDAO;
    private final UbicacionDAO ubicacionDAO;
    private final Sesion sesion;
    private final AutorizacionServicio autorizacionServicio;

    public StockServicio(CategoriaDAO categoriaDAO, ConfiguracionStockMinimoDAO configuracionDAO,
                         EquipoDAO equipoDAO, UbicacionDAO ubicacionDAO,
                         Sesion sesion, AutorizacionServicio autorizacionServicio) {
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO");
        this.configuracionDAO = Objects.requireNonNull(configuracionDAO, "configuracionDAO");
        this.equipoDAO = Objects.requireNonNull(equipoDAO, "equipoDAO");
        this.ubicacionDAO = Objects.requireNonNull(ubicacionDAO, "ubicacionDAO");
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.autorizacionServicio = Objects.requireNonNull(autorizacionServicio, "autorizacionServicio");
    }

    public ConfiguracionStockMinimo configurarMinimo(UUID categoriaId, String sede, int minimo) {
        autorizar(Permiso.CONFIGURAR_STOCK_MINIMO);
        if (minimo < 0) {
            throw new IllegalArgumentException("El minimo no puede ser negativo");
        }
        Categoria categoria = buscarCategoria(categoriaId);
        String sedeCanonica = buscarSede(sede);
        Optional<ConfiguracionStockMinimo> existente = configuracionDAO
                .buscarPorCategoriaYSede(categoria.getId(), sedeCanonica);
        if (existente.isPresent()) {
            ConfiguracionStockMinimo configuracion = existente.get();
            configuracion.setMinimo(minimo);
            configuracionDAO.actualizar(configuracion);
            return configuracionDAO.buscarPorId(configuracion.getId()).orElseThrow();
        }

        ConfiguracionStockMinimo configuracion = new ConfiguracionStockMinimo(
                UUID.randomUUID(), categoria.getId(), sedeCanonica, minimo);
        configuracionDAO.guardar(configuracion);
        return configuracionDAO.buscarPorId(configuracion.getId()).orElseThrow();
    }

    public EstadoStock consultarDisponibilidad(UUID categoriaId, String sede) {
        autorizar(Permiso.CONSULTAR_INVENTARIO);
        Categoria categoria = buscarCategoria(categoriaId);
        String sedeCanonica = buscarSede(sede);
        return calcular(categoria, sedeCanonica);
    }

    public List<EstadoStock> listarExistencias() {
        autorizar(Permiso.CONSULTAR_INVENTARIO);
        return listarExistenciasActuales();
    }

    public List<EstadoStock> consultarAlertasActuales() {
        autorizar(Permiso.CONSULTAR_ALERTAS);
        return listarExistenciasActuales().stream().filter(EstadoStock::tieneAlerta).toList();
    }

    List<EstadoStock> evaluarAfectadas(UUID categoriaId, List<String> sedes) {
        Categoria categoria = buscarCategoria(categoriaId);
        Map<String, String> sedesUnicas = new LinkedHashMap<>();
        for (String sede : sedes) {
            String canonica = buscarSede(sede);
            sedesUnicas.putIfAbsent(normalizar(canonica), canonica);
        }
        List<EstadoStock> resultados = new ArrayList<>(sedesUnicas.size());
        sedesUnicas.values().forEach(sede -> resultados.add(calcular(categoria, sede)));
        return List.copyOf(resultados);
    }

    String sedeCanonicaExistente(String sede) {
        return buscarSede(sede);
    }

    private List<EstadoStock> listarExistenciasActuales() {
        Map<ClaveStock, String> combinaciones = new LinkedHashMap<>();
        for (ConfiguracionStockMinimo configuracion : configuracionDAO.listar()) {
            Categoria categoria = buscarCategoria(configuracion.getCategoriaId());
            String claveSede = normalizar(configuracion.getSede());
            String sedeCanonica = buscarSede(configuracion.getSede());
            combinaciones.putIfAbsent(new ClaveStock(categoria.getId(), claveSede), sedeCanonica);
        }
        for (Equipo equipo : equipoDAO.listar()) {
            if (!equipo.isPresenteEnAlmacen()) {
                continue;
            }
            UUID ubicacionId = equipo.getUbicacionActualId()
                    .orElseThrow(() -> new IllegalStateException("Equipo presente sin ubicacion actual"));
            Ubicacion ubicacion = buscarUbicacion(ubicacionId);
            if (!ubicacion.isAlmacen()) {
                continue;
            }
            String sedeCanonica = buscarSede(ubicacion.getSede());
            combinaciones.putIfAbsent(new ClaveStock(equipo.getCategoriaId(), normalizar(sedeCanonica)), sedeCanonica);
        }

        List<EstadoStock> resultados = new ArrayList<>(combinaciones.size());
        combinaciones.forEach((clave, sede) -> resultados.add(calcular(buscarCategoria(clave.categoriaId()), sede)));
        return List.copyOf(resultados);
    }

    private EstadoStock calcular(Categoria categoria, String sedeCanonica) {
        String claveSede = normalizar(sedeCanonica);
        int totalPresente = 0;
        int disponible = 0;
        for (Equipo equipo : equipoDAO.listar()) {
            if (!equipo.getCategoriaId().equals(categoria.getId()) || !equipo.isPresenteEnAlmacen()) {
                continue;
            }
            UUID ubicacionId = equipo.getUbicacionActualId()
                    .orElseThrow(() -> new IllegalStateException("Equipo presente sin ubicacion actual"));
            Ubicacion ubicacion = buscarUbicacion(ubicacionId);
            if (!ubicacion.isAlmacen() || !normalizar(ubicacion.getSede()).equals(claveSede)) {
                continue;
            }
            totalPresente++;
            if (equipo.getEstado() == EstadoEquipo.DISPONIBLE) {
                disponible++;
            }
        }
        Optional<Integer> minimo = configuracionDAO.buscarPorCategoriaYSede(categoria.getId(), sedeCanonica)
                .map(ConfiguracionStockMinimo::getMinimo);
        return new EstadoStock(categoria.getId(), categoria.getNombre(), sedeCanonica,
                totalPresente, disponible, minimo);
    }

    private Categoria buscarCategoria(UUID categoriaId) {
        Objects.requireNonNull(categoriaId, "categoriaId");
        return categoriaDAO.buscarPorId(categoriaId)
                .orElseThrow(() -> new NoSuchElementException("No existe categoria con identificador " + categoriaId));
    }

    private Ubicacion buscarUbicacion(UUID ubicacionId) {
        return ubicacionDAO.buscarPorId(ubicacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe ubicacion con identificador " + ubicacionId));
    }

    private String buscarSede(String sede) {
        String clave = normalizar(sede);
        return ubicacionDAO.listar().stream()
                .map(Ubicacion::getSede)
                .filter(sedeExistente -> normalizar(sedeExistente).equals(clave))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No existe sede representada por una ubicacion: " + sede));
    }

    private String normalizar(String texto) {
        Objects.requireNonNull(texto, "sede no puede ser null");
        String normalizado = texto.strip();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException("sede no puede estar vacia");
        }
        return normalizado.toLowerCase(Locale.ROOT);
    }

    private void autorizar(Permiso permiso) {
        autorizacionServicio.exigirPermiso(permiso);
        sesion.exigirSesionActiva();
    }

    private record ClaveStock(UUID categoriaId, String sedeNormalizada) {
    }
}