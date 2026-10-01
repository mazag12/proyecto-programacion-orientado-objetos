package com.computototal.inventario.servicio;

import com.computototal.inventario.modelo.EstadoEquipo;
import java.util.Optional;
import java.util.UUID;

public record EquipoConsulta(
        UUID id,
        String codigo,
        String numeroSerie,
        String marca,
        String modelo,
        UUID categoriaId,
        String categoria,
        UUID proveedorId,
        String proveedor,
        EstadoEquipo estado,
        boolean presenteEnAlmacen,
        Optional<UbicacionResumen> ubicacionActual,
        Optional<UbicacionResumen> ultimaUbicacionAlmacen,
        Optional<String> destinoSalida) {

    public record UbicacionResumen(UUID id, String sede, String ambiente, String area, String piso, boolean almacen) {
    }
}