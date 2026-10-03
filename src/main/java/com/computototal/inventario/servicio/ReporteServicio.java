package com.computototal.inventario.servicio;

import com.computototal.inventario.dao.MovimientoDAO;
import com.computototal.inventario.modelo.Movimiento;
import com.computototal.inventario.validacion.ValidacionesEntrada;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ReporteServicio {
    private final MovimientoDAO movimientoDAO;
    private final StockServicio stockServicio;
    private final Sesion sesion;
    private final AutorizacionServicio autorizacionServicio;

    public ReporteServicio(MovimientoDAO movimientoDAO, StockServicio stockServicio,
                           Sesion sesion, AutorizacionServicio autorizacionServicio) {
        this.movimientoDAO = Objects.requireNonNull(movimientoDAO, "movimientoDAO");
        this.stockServicio = Objects.requireNonNull(stockServicio, "stockServicio");
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.autorizacionServicio = Objects.requireNonNull(autorizacionServicio, "autorizacionServicio");
    }

    public ReporteMovimientos generarGlobal(LocalDate inicio, LocalDate fin) {
        autorizar();
        validarRango(inicio, fin);
        return generar(inicio, fin, Optional.empty());
    }

    public ReporteMovimientos generarPorSede(LocalDate inicio, LocalDate fin, String sede) {
        autorizar();
        validarRango(inicio, fin);
        String sedeCanonica = stockServicio.sedeCanonicaExistente(sede);
        return generar(inicio, fin, Optional.of(sedeCanonica));
    }

    private ReporteMovimientos generar(LocalDate inicio, LocalDate fin, Optional<String> sede) {
        List<Movimiento> todos = movimientoDAO.listar();
        List<Movimiento> periodo = todos.stream()
                .filter(movimiento -> dentroDelPeriodo(movimiento, inicio, fin))
                .filter(movimiento -> afectaSede(movimiento, sede))
                .toList();

        Map<UUID, Integer> saldoInicialPorCategoria = reconstruir(todos, inicio, sede, false);
        Map<UUID, Integer> saldoFinalPorCategoria = reconstruir(todos, fin, sede, true);
        Map<UUID, ContadoresCategoria> contadores = contarMovimientos(periodo, sede);
        Map<UUID, String> nombresCategoria = obtenerNombresCategoria(todos);
        LinkedHashSet<UUID> categorias = new LinkedHashSet<>();
        categorias.addAll(saldoInicialPorCategoria.keySet());
        categorias.addAll(saldoFinalPorCategoria.keySet());
        categorias.addAll(contadores.keySet());

        List<SaldoCategoria> saldosPorCategoria = new ArrayList<>(categorias.size());
        for (UUID categoriaId : categorias) {
            ContadoresCategoria conteos = contadores.getOrDefault(categoriaId, new ContadoresCategoria());
            saldosPorCategoria.add(new SaldoCategoria(categoriaId, nombresCategoria.getOrDefault(categoriaId, "Categoria"),
                    saldoInicialPorCategoria.getOrDefault(categoriaId, 0), conteos.entradas, conteos.salidas,
                    conteos.anulacionesAlta,
                    conteos.trasladosRecibidos, conteos.trasladosEnviados,
                    saldoFinalPorCategoria.getOrDefault(categoriaId, 0)));
        }

        int entradas = saldosPorCategoria.stream().mapToInt(SaldoCategoria::entradas).sum();
        int salidas = saldosPorCategoria.stream().mapToInt(SaldoCategoria::salidas).sum();
        int anulacionesAlta = saldosPorCategoria.stream().mapToInt(SaldoCategoria::anulacionesAlta).sum();
        int recibidos = saldosPorCategoria.stream().mapToInt(SaldoCategoria::trasladosRecibidos).sum();
        int enviados = saldosPorCategoria.stream().mapToInt(SaldoCategoria::trasladosEnviados).sum();
        int inicial = saldoInicialPorCategoria.values().stream().mapToInt(Integer::intValue).sum();
        int finalPeriodo = saldoFinalPorCategoria.values().stream().mapToInt(Integer::intValue).sum();

        List<EstadoStock> actuales = stockServicio.listarExistencias().stream()
                .filter(estado -> sede.isEmpty() || mismaSede(estado.sede(), sede.get()))
                .toList();
        return new ReporteMovimientos(inicio, fin, sede, periodo, entradas, salidas, anulacionesAlta,
            recibidos, enviados, inicial, finalPeriodo, saldosPorCategoria, actuales);
    }

    private Map<UUID, Integer> reconstruir(List<Movimiento> movimientos, LocalDate corte,
                                           Optional<String> sede, boolean incluirDiaDeCorte) {
        Map<UUID, Integer> saldos = new LinkedHashMap<>();
        for (Movimiento movimiento : movimientos) {
            LocalDate fecha = movimiento.getFechaHora().toLocalDate();
            boolean hastaCorte = incluirDiaDeCorte ? !fecha.isAfter(corte) : fecha.isBefore(corte);
            if (hastaCorte) {
                aplicarSaldo(movimiento, sede, saldos);
            }
        }
        return saldos;
    }

    private void aplicarSaldo(Movimiento movimiento, Optional<String> sede, Map<UUID, Integer> saldos) {
        UUID categoriaId = movimiento.getEquipo().categoriaId();
        switch (movimiento.getTipo()) {
            case INGRESO -> agregarSiCorresponde(saldos, categoriaId,
                    movimiento.getUbicacionDestino().orElseThrow().sede(), 1, sede);
            case SALIDA -> agregarSiCorresponde(saldos, categoriaId,
                    movimiento.getUbicacionOrigen().orElseThrow().sede(), -1, sede);
            case TRASLADO -> {
                agregarSiCorresponde(saldos, categoriaId,
                        movimiento.getUbicacionOrigen().orElseThrow().sede(), -1, sede);
                agregarSiCorresponde(saldos, categoriaId,
                        movimiento.getUbicacionDestino().orElseThrow().sede(), 1, sede);
            }
            case CAMBIO_UBICACION -> {
                if (movimiento.isPresenciaAnterior()) {
                    agregarSiCorresponde(saldos, categoriaId,
                            movimiento.getUbicacionOrigen().orElseThrow().sede(), -1, sede);
                }
                if (movimiento.isPresenciaNueva()) {
                    agregarSiCorresponde(saldos, categoriaId,
                            movimiento.getUbicacionDestino().orElseThrow().sede(), 1, sede);
                }
            }
            case CAMBIO_ESTADO -> {
            }
                case ANULACION_ALTA -> agregarSiCorresponde(saldos, categoriaId,
                    movimiento.getUbicacionOrigen().orElseThrow().sede(), -1, sede);
        }
    }

    private void agregarSiCorresponde(Map<UUID, Integer> saldos, UUID categoriaId,
                                      String sedeMovimiento, int delta, Optional<String> sedeFiltro) {
        if (sedeFiltro.isEmpty() || mismaSede(sedeMovimiento, sedeFiltro.get())) {
            saldos.merge(categoriaId, delta, Integer::sum);
        }
    }

    private Map<UUID, ContadoresCategoria> contarMovimientos(List<Movimiento> movimientos, Optional<String> sede) {
        Map<UUID, ContadoresCategoria> conteos = new LinkedHashMap<>();
        for (Movimiento movimiento : movimientos) {
            UUID categoriaId = movimiento.getEquipo().categoriaId();
            ContadoresCategoria contador = conteos.computeIfAbsent(categoriaId, ignorado -> new ContadoresCategoria());
            switch (movimiento.getTipo()) {
                case INGRESO -> contador.entradas++;
                case SALIDA -> contador.salidas++;
                case TRASLADO -> {
                    if (sede.isEmpty() || mismaSede(movimiento.getUbicacionDestino().orElseThrow().sede(), sede.get())) {
                        contador.trasladosRecibidos++;
                    }
                    if (sede.isEmpty() || mismaSede(movimiento.getUbicacionOrigen().orElseThrow().sede(), sede.get())) {
                        contador.trasladosEnviados++;
                    }
                }
                case CAMBIO_UBICACION -> {
                    if (!movimiento.isPresenciaAnterior() && movimiento.isPresenciaNueva()) {
                        contador.entradas++;
                    } else if (movimiento.isPresenciaAnterior() && !movimiento.isPresenciaNueva()) {
                        contador.salidas++;
                    }
                }
                case CAMBIO_ESTADO -> {
                }
                case ANULACION_ALTA -> contador.anulacionesAlta++;
            }
        }
        return conteos;
    }

    private Map<UUID, String> obtenerNombresCategoria(List<Movimiento> movimientos) {
        Map<UUID, String> nombres = new LinkedHashMap<>();
        movimientos.forEach(movimiento -> nombres.putIfAbsent(
                movimiento.getEquipo().categoriaId(), movimiento.getEquipo().nombreCategoria()));
        return nombres;
    }

    private boolean dentroDelPeriodo(Movimiento movimiento, LocalDate inicio, LocalDate fin) {
        LocalDate fecha = movimiento.getFechaHora().toLocalDate();
        return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
    }

    private boolean afectaSede(Movimiento movimiento, Optional<String> sede) {
        if (sede.isEmpty()) {
            return true;
        }
        return movimiento.getUbicacionOrigen().map(Ubicacion -> mismaSede(Ubicacion.sede(), sede.get())).orElse(false)
                || movimiento.getUbicacionDestino().map(Ubicacion -> mismaSede(Ubicacion.sede(), sede.get())).orElse(false);
    }

    private boolean mismaSede(String una, String otra) {
        return una.strip().equalsIgnoreCase(otra.strip());
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        ValidacionesEntrada.rangoFechas(inicio, fin);
    }

    private void autorizar() {
        autorizacionServicio.exigirPermiso(Permiso.CONSULTAR_REPORTES);
        sesion.exigirSesionActiva();
    }

    private static final class ContadoresCategoria {
        private int entradas;
        private int salidas;
        private int anulacionesAlta;
        private int trasladosRecibidos;
        private int trasladosEnviados;
    }
}