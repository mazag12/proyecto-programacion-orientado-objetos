package com.computototal.inventario.modelo;

import java.util.UUID;

public class Ubicacion {
    private final UUID id;
    private final boolean almacen;
    private String sede;
    private String ambiente;
    private String area;
    private String piso;

    public Ubicacion(UUID id, String sede, String ambiente, String area, String piso, boolean almacen) {
        this.id = ValidacionDominio.identificador(id, "id");
        this.sede = ValidacionDominio.textoObligatorio(sede, "sede");
        this.ambiente = ValidacionDominio.textoObligatorio(ambiente, "ambiente");
        this.area = ValidacionDominio.textoObligatorio(area, "area");
        this.piso = ValidacionDominio.textoObligatorio(piso, "piso");
        this.almacen = almacen;
    }

    public UUID getId() {
        return id;
    }

    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = ValidacionDominio.textoObligatorio(sede, "sede");
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ValidacionDominio.textoObligatorio(ambiente, "ambiente");
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = ValidacionDominio.textoObligatorio(area, "area");
    }

    public String getPiso() {
        return piso;
    }

    public void setPiso(String piso) {
        this.piso = ValidacionDominio.textoObligatorio(piso, "piso");
    }

    public boolean isAlmacen() {
        return almacen;
    }

    public String obtenerDescripcion() {
        return sede + " / " + ambiente + " / " + area + " / " + piso;
    }
}