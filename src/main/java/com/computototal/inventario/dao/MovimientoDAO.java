package com.computototal.inventario.dao;

import com.computototal.inventario.modelo.Movimiento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoDAO {
    void insertar(Movimiento movimiento);

    Optional<Movimiento> buscarPorId(UUID id);

    List<Movimiento> listar();

    List<Movimiento> listarPorEquipo(UUID equipoId);

    List<Movimiento> listarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin);
}