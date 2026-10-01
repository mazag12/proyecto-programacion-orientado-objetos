package com.computototal.inventario.modelo;

import java.util.Optional;
import java.util.UUID;

public class Equipo {
    private final UUID id;
    private final String codigo;
    private final String numeroSerie;
    private final UUID categoriaId;
    private final UUID proveedorId;
    private String marca;
    private String modelo;
    private EstadoEquipo estado;
    private boolean presenteEnAlmacen;
    private Optional<UUID> ubicacionActualId;
    private Optional<UUID> ultimaUbicacionAlmacenId;
    private Optional<String> destinoSalida;

    public Equipo(UUID id, String codigo, String numeroSerie, String marca, String modelo,
                  UUID categoriaId, UUID proveedorId, EstadoEquipo estadoInicial) {
        this.id = ValidacionDominio.identificador(id, "id");
        this.codigo = ValidacionDominio.textoObligatorio(codigo, "codigo");
        this.numeroSerie = ValidacionDominio.textoObligatorio(numeroSerie, "numeroSerie");
        this.marca = ValidacionDominio.textoObligatorio(marca, "marca");
        this.modelo = ValidacionDominio.textoObligatorio(modelo, "modelo");
        this.categoriaId = ValidacionDominio.identificador(categoriaId, "categoriaId");
        this.proveedorId = ValidacionDominio.identificador(proveedorId, "proveedorId");
        this.estado = ValidacionDominio.requerido(estadoInicial, "estadoInicial");
        this.presenteEnAlmacen = false;
        this.ubicacionActualId = Optional.empty();
        this.ultimaUbicacionAlmacenId = Optional.empty();
        this.destinoSalida = Optional.empty();
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = ValidacionDominio.textoObligatorio(marca, "marca");
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = ValidacionDominio.textoObligatorio(modelo, "modelo");
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public EstadoEquipo getEstado() {
        return estado;
    }

    public boolean isPresenteEnAlmacen() {
        return presenteEnAlmacen;
    }

    public Optional<UUID> getUbicacionActualId() {
        return ubicacionActualId;
    }

    public Optional<UUID> getUltimaUbicacionAlmacenId() {
        return ultimaUbicacionAlmacenId;
    }

    public Optional<String> getDestinoSalida() {
        return destinoSalida;
    }

    public void registrarIngreso(Ubicacion ubicacionAlmacen) {
        verificarNoDadoDeBaja();
        Ubicacion ubicacion = ValidacionDominio.requerido(ubicacionAlmacen, "ubicacionAlmacen");
        if (!ubicacion.isAlmacen()) {
            throw new IllegalArgumentException("El ingreso requiere una ubicacion de almacen");
        }
        if (presenteEnAlmacen) {
            throw new IllegalStateException("El equipo ya esta presente en almacen");
        }
        presenteEnAlmacen = true;
        ubicacionActualId = Optional.of(ubicacion.getId());
        ultimaUbicacionAlmacenId = Optional.of(ubicacion.getId());
        destinoSalida = Optional.empty();
        estado = EstadoEquipo.DISPONIBLE;
    }

    public void registrarSalida(String destino) {
        verificarNoDadoDeBaja();
        String destinoNormalizado = ValidacionDominio.textoObligatorio(destino, "destino");
        if (!presenteEnAlmacen) {
            throw new IllegalStateException("Solo puede salir un equipo presente en almacen");
        }
        if (estado != EstadoEquipo.DISPONIBLE) {
            throw new IllegalStateException("Solo puede salir un equipo disponible");
        }
        presenteEnAlmacen = false;
        ubicacionActualId = Optional.empty();
        destinoSalida = Optional.of(destinoNormalizado);
        estado = EstadoEquipo.EN_USO;
    }

    public void trasladarAAlmacen(Ubicacion destinoAlmacen) {
        verificarNoDadoDeBaja();
        Ubicacion destino = ValidacionDominio.requerido(destinoAlmacen, "destinoAlmacen");
        if (!destino.isAlmacen()) {
            throw new IllegalArgumentException("El traslado requiere una ubicacion de almacen");
        }
        if (!presenteEnAlmacen) {
            throw new IllegalStateException("Solo puede trasladarse un equipo presente en almacen");
        }
        if (ubicacionActualId.filter(destino.getId()::equals).isPresent()) {
            throw new IllegalArgumentException("El equipo ya se encuentra en ese almacen");
        }
        ubicacionActualId = Optional.of(destino.getId());
        ultimaUbicacionAlmacenId = Optional.of(destino.getId());
        destinoSalida = Optional.empty();
    }

    public void cambiarUbicacion(Ubicacion nuevaUbicacion) {
        verificarNoDadoDeBaja();
        Ubicacion ubicacion = ValidacionDominio.requerido(nuevaUbicacion, "nuevaUbicacion");
        if (ubicacionActualId.filter(ubicacion.getId()::equals).isPresent()) {
            throw new IllegalArgumentException("El equipo ya se encuentra en esa ubicacion");
        }
        ubicacionActualId = Optional.of(ubicacion.getId());
        presenteEnAlmacen = ubicacion.isAlmacen();
        if (ubicacion.isAlmacen()) {
            ultimaUbicacionAlmacenId = Optional.of(ubicacion.getId());
            destinoSalida = Optional.empty();
        } else {
            destinoSalida = Optional.of(ubicacion.obtenerDescripcion());
        }
    }

    public void cambiarEstado(EstadoEquipo nuevoEstado) {
        EstadoEquipo estadoValidado = ValidacionDominio.requerido(nuevoEstado, "nuevoEstado");
        verificarNoDadoDeBaja();
        if (estado == estadoValidado) {
            throw new IllegalArgumentException("El equipo ya tiene ese estado");
        }
        if (!presenteEnAlmacen && estadoValidado == EstadoEquipo.DISPONIBLE) {
            throw new IllegalStateException("Un equipo fuera del almacen debe reingresar para estar disponible");
        }
        estado = estadoValidado;
    }

    public Equipo copiar() {
        Equipo copia = new Equipo(id, codigo, numeroSerie, marca, modelo, categoriaId, proveedorId, estado);
        copia.presenteEnAlmacen = presenteEnAlmacen;
        copia.ubicacionActualId = ubicacionActualId;
        copia.ultimaUbicacionAlmacenId = ultimaUbicacionAlmacenId;
        copia.destinoSalida = destinoSalida;
        return copia;
    }

    private void verificarNoDadoDeBaja() {
        if (estado == EstadoEquipo.DE_BAJA) {
            throw new IllegalStateException("Un equipo dado de baja no puede reactivarse ni moverse");
        }
    }
}