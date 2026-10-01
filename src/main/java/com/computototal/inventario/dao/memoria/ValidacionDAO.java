package com.computototal.inventario.dao.memoria;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

final class ValidacionDAO {
    private ValidacionDAO() {
    }

    static UUID id(UUID valor) {
        return Objects.requireNonNull(valor, "El identificador no puede ser null");
    }

    static <T> T entidad(T valor, String nombre) {
        return Objects.requireNonNull(valor, nombre + " no puede ser null");
    }

    static String textoClave(String valor, String nombre) {
        Objects.requireNonNull(valor, nombre + " no puede ser null");
        String normalizado = valor.strip();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException(nombre + " no puede estar vacio");
        }
        return normalizado.toLowerCase(Locale.ROOT);
    }

    static IllegalArgumentException duplicado(String nombre, String valor) {
        return new IllegalArgumentException("Ya existe un registro con " + nombre + ": " + valor);
    }

    static NoSuchElementException inexistente(String entidad, UUID id) {
        return new NoSuchElementException("No existe " + entidad + " con identificador " + id);
    }
}