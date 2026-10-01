package com.computototal.inventario.servicio;

import java.util.Objects;
import java.util.UUID;

public record SaldoCategoria(UUID categoriaId, String categoria, int saldoInicial,
                             int entradas, int salidas,
                             int trasladosRecibidos, int trasladosEnviados,
                             int saldoFinal) {
    public SaldoCategoria {
        Objects.requireNonNull(categoriaId, "categoriaId");
        Objects.requireNonNull(categoria, "categoria");
    }
}