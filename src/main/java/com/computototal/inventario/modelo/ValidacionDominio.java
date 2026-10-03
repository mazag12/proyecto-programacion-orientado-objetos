package com.computototal.inventario.modelo;

import java.util.UUID;

import com.computototal.inventario.validacion.ValidacionesEntrada;

final class ValidacionDominio {
    private ValidacionDominio() {
    }

    static String textoObligatorio(String valor, String nombreCampo) {
        return ValidacionesEntrada.textoObligatorio(valor, nombreCampo);
    }

    static String numeroSerie(String valor) {
        return ValidacionesEntrada.numeroSerie(valor);
    }

    static UUID identificador(UUID valor, String nombreCampo) {
        return ValidacionesEntrada.identificador(valor, nombreCampo);
    }

    static <T> T requerido(T valor, String nombreCampo) {
        return ValidacionesEntrada.requerido(valor, nombreCampo);
    }
}