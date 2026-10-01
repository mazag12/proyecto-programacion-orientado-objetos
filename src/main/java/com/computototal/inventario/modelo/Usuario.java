package com.computototal.inventario.modelo;

import java.util.UUID;

public class Usuario {
    private final UUID id;
    private String nombreUsuario;
    private byte[] hashContrasena;
    private byte[] salContrasena;
    private Rol rol;

    public Usuario(UUID id, String nombreUsuario, byte[] hashContrasena, byte[] salContrasena, Rol rol) {
        this.id = ValidacionDominio.identificador(id, "id");
        this.nombreUsuario = ValidacionDominio.textoObligatorio(nombreUsuario, "nombreUsuario");
        this.hashContrasena = copiarBytesNoVacios(hashContrasena, "hashContrasena");
        this.salContrasena = copiarBytesNoVacios(salContrasena, "salContrasena");
        this.rol = ValidacionDominio.requerido(rol, "rol");
    }

    public UUID getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = ValidacionDominio.textoObligatorio(nombreUsuario, "nombreUsuario");
    }

    public byte[] getHashContrasena() {
        return hashContrasena.clone();
    }

    public byte[] getSalContrasena() {
        return salContrasena.clone();
    }

    public void actualizarCredenciales(byte[] hashContrasena, byte[] salContrasena) {
        this.hashContrasena = copiarBytesNoVacios(hashContrasena, "hashContrasena");
        this.salContrasena = copiarBytesNoVacios(salContrasena, "salContrasena");
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = ValidacionDominio.requerido(rol, "rol");
    }

    private byte[] copiarBytesNoVacios(byte[] valor, String nombreCampo) {
        if (valor == null || valor.length == 0) {
            throw new IllegalArgumentException(nombreCampo + " no puede ser null ni estar vacio");
        }
        return valor.clone();
    }
}