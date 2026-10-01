package com.computototal.inventario.modelo;

import java.util.UUID;

public class Categoria {
    private final UUID id;
    private String nombre;
    private String descripcion;

    public Categoria(UUID id, String nombre, String descripcion) {
        this.id = ValidacionDominio.identificador(id, "id");
        this.nombre = ValidacionDominio.textoObligatorio(nombre, "nombre");
        this.descripcion = ValidacionDominio.textoObligatorio(descripcion, "descripcion");
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = ValidacionDominio.textoObligatorio(descripcion, "descripcion");
    }
}