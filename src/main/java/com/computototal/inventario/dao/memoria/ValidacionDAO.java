package com.computototal.inventario.dao.memoria;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

import com.computototal.inventario.validacion.ValidacionesEntrada;

final class ValidacionDAO {
    private ValidacionDAO() {
    }

    static UUID id(UUID valor) {
        return ValidacionesEntrada.identificador(valor, "El identificador");
    }

    static <T> T entidad(T valor, String nombre) {
        return Objects.requireNonNull(valor, nombre + " no puede ser null");
    }

    static String textoClave(String valor, String nombre) {
        return ValidacionesEntrada.textoClave(valor, nombre);
    }

    static IllegalArgumentException duplicado(String nombre, String valor) {
        return new IllegalArgumentException("Ya existe un registro con " + nombre + ": " + valor);
    }

    static NoSuchElementException inexistente(String entidad, UUID id) {
        return new NoSuchElementException("No existe " + entidad + " con identificador " + id);
    }
}