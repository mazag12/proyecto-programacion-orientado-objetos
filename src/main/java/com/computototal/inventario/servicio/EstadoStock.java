package com.computototal.inventario.servicio;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record EstadoStock(UUID categoriaId, String categoria, String sede, int totalPresente,
                          int disponible, Optional<Integer> minimoConfigurado) {
    public EstadoStock {
        Objects.requireNonNull(categoriaId, "categoriaId");
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(sede, "sede");
        Objects.requireNonNull(minimoConfigurado, "minimoConfigurado");
        if (totalPresente < 0 || disponible < 0 || disponible > totalPresente) {
            throw new IllegalArgumentException("Los conteos de stock no son validos");
        }
    }

    public boolean tieneAlerta() {
        return minimoConfigurado.filter(minimo -> disponible < minimo).isPresent();
    }
}