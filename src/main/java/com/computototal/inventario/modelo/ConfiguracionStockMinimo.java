package com.computototal.inventario.modelo;

import java.util.UUID;

public class ConfiguracionStockMinimo {
    private final UUID id;
    private final UUID categoriaId;
    private final String sede;
    private int minimo;

    public ConfiguracionStockMinimo(UUID id, UUID categoriaId, String sede, int minimo) {
        this.id = ValidacionDominio.identificador(id, "id");
        this.categoriaId = ValidacionDominio.identificador(categoriaId, "categoriaId");
        this.sede = ValidacionDominio.textoObligatorio(sede, "sede");
        this.minimo = validarMinimo(minimo);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public String getSede() {
        return sede;
    }

    public int getMinimo() {
        return minimo;
    }

    public void setMinimo(int minimo) {
        this.minimo = validarMinimo(minimo);
    }

    private static int validarMinimo(int minimo) {
        if (minimo < 0) {
            throw new IllegalArgumentException("minimo no puede ser negativo");
        }
        return minimo;
    }
}