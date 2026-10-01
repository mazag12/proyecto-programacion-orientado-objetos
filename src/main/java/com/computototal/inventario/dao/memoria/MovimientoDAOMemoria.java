package com.computototal.inventario.dao.memoria;

import com.computototal.inventario.dao.MovimientoDAO;
import com.computototal.inventario.modelo.Movimiento;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MovimientoDAOMemoria implements MovimientoDAO {
    private static final Comparator<Movimiento> ORDEN_CRONOLOGICO = Comparator
            .comparing(Movimiento::getFechaHora);

    private final Map<UUID, Movimiento> movimientos = new LinkedHashMap<>();

    @Override
    public void insertar(Movimiento movimiento) {
        Movimiento validado = ValidacionDAO.entidad(movimiento, "movimiento");
        UUID id = ValidacionDAO.id(validado.getId());
        if (movimientos.containsKey(id)) {
            throw ValidacionDAO.duplicado("identificador de movimiento", id.toString());
        }
        movimientos.put(id, CopiasModelo.copiar(validado));
    }

    @Override
    public Optional<Movimiento> buscarPorId(UUID id) {
        return Optional.ofNullable(movimientos.get(ValidacionDAO.id(id))).map(CopiasModelo::copiar);
    }

    @Override
    public List<Movimiento> listar() {
        return ordenar(movimientos.values());
    }

    @Override
    public List<Movimiento> listarPorEquipo(UUID equipoId) {
        UUID idValidado = ValidacionDAO.id(equipoId);
        return ordenar(movimientos.values().stream()
                .filter(movimiento -> movimiento.getEquipo().id().equals(idValidado))
                .toList());
    }

    @Override
    public List<Movimiento> listarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime inicioValidado = ValidacionDAO.entidad(inicio, "inicio");
        LocalDateTime finValidado = ValidacionDAO.entidad(fin, "fin");
        if (inicioValidado.isAfter(finValidado)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        return ordenar(movimientos.values().stream()
                .filter(movimiento -> !movimiento.getFechaHora().isBefore(inicioValidado)
                        && !movimiento.getFechaHora().isAfter(finValidado))
                .toList());
    }

    private List<Movimiento> ordenar(Iterable<Movimiento> origen) {
        List<Movimiento> resultado = new ArrayList<>();
        origen.forEach(movimiento -> resultado.add(CopiasModelo.copiar(movimiento)));
        resultado.sort(ORDEN_CRONOLOGICO);
        return List.copyOf(resultado);
    }
}