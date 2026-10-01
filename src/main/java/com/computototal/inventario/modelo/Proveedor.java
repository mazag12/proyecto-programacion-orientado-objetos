package com.computototal.inventario.modelo;

import java.util.UUID;
import java.util.regex.Pattern;

public class Proveedor {
    private static final Pattern FORMATO_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UUID id;
    private String nombre;
    private String telefono;
    private String correoElectronico;

    public Proveedor(UUID id, String nombre, String telefono, String correoElectronico) {
        this.id = ValidacionDominio.identificador(id, "id");
        this.nombre = ValidacionDominio.textoObligatorio(nombre, "nombre");
        this.telefono = ValidacionDominio.textoObligatorio(telefono, "telefono");
        this.correoElectronico = validarCorreo(correoElectronico);
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
        this.telefono = ValidacionDominio.textoObligatorio(telefono, "telefono");
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = validarCorreo(correoElectronico);
    }

    private String validarCorreo(String correo) {
        String correoNormalizado = ValidacionDominio.textoObligatorio(correo, "correoElectronico");
        if (!FORMATO_CORREO.matcher(correoNormalizado).matches()) {
            throw new IllegalArgumentException("correoElectronico no tiene un formato valido");
        }
        return correoNormalizado;
    }
}