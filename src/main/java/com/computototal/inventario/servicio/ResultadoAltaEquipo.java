package com.computototal.inventario.servicio;

import java.util.Objects;
import java.util.Optional;

public record ResultadoAltaEquipo(EquipoConsulta equipo, Optional<ResultadoMovimiento> ingresoInicial) {
    public ResultadoAltaEquipo {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(ingresoInicial, "ingresoInicial");
    }
}