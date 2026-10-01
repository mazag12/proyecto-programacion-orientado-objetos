package com.computototal.inventario.servicio;

import com.computototal.inventario.modelo.Rol;
import java.util.Objects;
import java.util.UUID;

public record SesionUsuario(UUID id, String nombreUsuario, Rol rol) {
    public SesionUsuario {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombreUsuario, "nombreUsuario");
        nombreUsuario = nombreUsuario.strip();
        if (nombreUsuario.isEmpty()) {
            throw new IllegalArgumentException("nombreUsuario no puede estar vacio");
        }
        Objects.requireNonNull(rol, "rol");
    }
}