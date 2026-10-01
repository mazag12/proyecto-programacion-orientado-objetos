package com.computototal.inventario.modelo;

import java.util.UUID;

import com.computototal.inventario.validacion.ValidacionesEntrada;

public class Proveedor {
    private final UUID id;
    private String nombre;
    private String telefono;
    private String correoElectronico;

    public Proveedor(UUID id, String nombre, String telefono, String correoElectronico) {
        this.id = ValidacionDominio.identificador(id, "id");
        this.nombre = ValidacionDominio.textoObligatorio(nombre, "nombre");
        this.telefono = ValidacionesEntrada.telefono(telefono);
        this.correoElectronico = ValidacionesEntrada.correoElectronico(correoElectronico);
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = ValidacionDominio.textoObligatorio(nombre, "nombre");
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = ValidacionesEntrada.telefono(telefono);
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = ValidacionesEntrada.correoElectronico(correoElectronico);
    }
}