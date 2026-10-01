package com.computototal.inventario.servicio;

import com.computototal.inventario.modelo.Movimiento;
import java.util.List;
import java.util.Objects;

public record ResultadoMovimiento(Movimiento movimiento, List<EstadoStock> estadosStockAfectados) {
    public ResultadoMovimiento {
        Objects.requireNonNull(movimiento, "movimiento");
        estadosStockAfectados = List.copyOf(estadosStockAfectados);
    }
}