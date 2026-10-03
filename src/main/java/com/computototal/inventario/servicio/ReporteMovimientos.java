package com.computototal.inventario.servicio;

import com.computototal.inventario.modelo.Movimiento;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ReporteMovimientos(LocalDate inicio, LocalDate fin, Optional<String> sede,
                                 List<Movimiento> movimientos,
                                 int entradas, int salidas, int anulacionesAlta,
                                 int trasladosRecibidos, int trasladosEnviados,
                                 int saldoInicial, int saldoFinal,
                                 List<SaldoCategoria> saldosPorCategoria,
                                 List<EstadoStock> existenciasActuales) {
    public ReporteMovimientos {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        Objects.requireNonNull(sede, "sede");
        movimientos = List.copyOf(movimientos);
        saldosPorCategoria = List.copyOf(saldosPorCategoria);
        existenciasActuales = List.copyOf(existenciasActuales);
    }
}