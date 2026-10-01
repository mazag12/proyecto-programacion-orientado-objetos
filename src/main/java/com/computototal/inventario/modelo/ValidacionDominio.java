package com.computototal.inventario.modelo;

import java.util.Objects;
import java.util.UUID;

final class ValidacionDominio {
    private ValidacionDominio() {
    }

    static String textoObligatorio(String valor, String nombreCampo) {
        Objects.requireNonNull(valor, nombreCampo + " no puede ser null");
        String valorNormalizado = valor.strip();
        if (valorNormalizado.isEmpty()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacio");
        }
        return valorNormalizado;
    }

    static UUID identificador(UUID valor, String nombreCampo) {
        return Objects.requireNonNull(valor, nombreCampo + " no puede ser null");
    }

    static <T> T requerido(T valor, String nombreCampo) {
        return Objects.requireNonNull(valor, nombreCampo + " no puede ser null");
    }
}