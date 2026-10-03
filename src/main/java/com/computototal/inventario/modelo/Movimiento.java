package com.computototal.inventario.modelo;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public final class Movimiento {
    private final UUID id;
    private final LocalDateTime fechaHora;
    private final TipoMovimiento tipo;
    private final String motivo;
    private final InstantaneaEquipo equipo;
    private final UUID responsableId;
    private final String nombreResponsable;
    private final Optional<InstantaneaUbicacion> ubicacionOrigen;
    private final Optional<InstantaneaUbicacion> ubicacionDestino;
    private final Optional<String> destinoExterno;
    private final Optional<EstadoEquipo> estadoAnterior;
    private final Optional<EstadoEquipo> estadoNuevo;
    private final boolean presenciaAnterior;
    private final boolean presenciaNueva;

    public Movimiento(UUID id, LocalDateTime fechaHora, TipoMovimiento tipo, String motivo, InstantaneaEquipo equipo,
                      UUID responsableId, String nombreResponsable,
                      Optional<InstantaneaUbicacion> ubicacionOrigen,
                      Optional<InstantaneaUbicacion> ubicacionDestino,
                      Optional<String> destinoExterno, Optional<EstadoEquipo> estadoAnterior,
                      Optional<EstadoEquipo> estadoNuevo, boolean presenciaAnterior, boolean presenciaNueva) {
        this.id = ValidacionDominio.identificador(id, "id");
        this.fechaHora = ValidacionDominio.requerido(fechaHora, "fechaHora");
        this.tipo = ValidacionDominio.requerido(tipo, "tipo");
        this.motivo = ValidacionDominio.textoObligatorio(motivo, "motivo");
        this.equipo = ValidacionDominio.requerido(equipo, "equipo");
        this.responsableId = ValidacionDominio.identificador(responsableId, "responsableId");
        this.nombreResponsable = ValidacionDominio.textoObligatorio(nombreResponsable, "nombreResponsable");
        this.ubicacionOrigen = ValidacionDominio.requerido(ubicacionOrigen, "ubicacionOrigen");
        this.ubicacionDestino = ValidacionDominio.requerido(ubicacionDestino, "ubicacionDestino");
        this.destinoExterno = ValidacionDominio.requerido(destinoExterno, "destinoExterno")
                .map(destino -> ValidacionDominio.textoObligatorio(destino, "destinoExterno"));
        this.estadoAnterior = ValidacionDominio.requerido(estadoAnterior, "estadoAnterior");
        this.estadoNuevo = ValidacionDominio.requerido(estadoNuevo, "estadoNuevo");
        this.presenciaAnterior = presenciaAnterior;
        this.presenciaNueva = presenciaNueva;
        validarDatosPorTipo();
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public String getMotivo() {
        return motivo;
    }

    public InstantaneaEquipo getEquipo() {
        return equipo;
    }

    public UUID getResponsableId() {
        return responsableId;
    }

    public String getNombreResponsable() {
        return nombreResponsable;
    }

    public Optional<InstantaneaUbicacion> getUbicacionOrigen() {
        return ubicacionOrigen;
    }

    public Optional<InstantaneaUbicacion> getUbicacionDestino() {
        return ubicacionDestino;
    }

    public Optional<String> getDestinoExterno() {
        return destinoExterno;
    }

    public Optional<EstadoEquipo> getEstadoAnterior() {
        return estadoAnterior;
    }

    public Optional<EstadoEquipo> getEstadoNuevo() {
        return estadoNuevo;
    }

    public boolean isPresenciaAnterior() {
        return presenciaAnterior;
    }

    public boolean isPresenciaNueva() {
        return presenciaNueva;
    }

    private void validarDatosPorTipo() {
        switch (tipo) {
            case INGRESO -> {
                exigir(!ubicacionOrigen.isPresent() && ubicacionDestino.filter(InstantaneaUbicacion::almacen).isPresent()
                                && destinoExterno.isEmpty() && !presenciaAnterior && presenciaNueva
                        && estadoAnterior.isPresent() && estadoAnterior.get() != EstadoEquipo.DE_BAJA
                        && estadoNuevo.orElse(null) == EstadoEquipo.DISPONIBLE,
                    "Un ingreso requiere origen fuera de almacen, destino de almacen y estados validos");
            }
            case SALIDA -> {
                exigir(ubicacionOrigen.filter(InstantaneaUbicacion::almacen).isPresent()
                                && ubicacionDestino.isEmpty() && destinoExterno.isPresent()
                                && presenciaAnterior && !presenciaNueva
                        && estadoAnterior.orElse(null) == EstadoEquipo.DISPONIBLE
                        && estadoNuevo.orElse(null) == EstadoEquipo.EN_USO,
                    "Una salida requiere equipo disponible, ubicacion de almacen y destino externo");
            }
            case TRASLADO -> exigir(ubicacionOrigen.filter(InstantaneaUbicacion::almacen).isPresent()
                            && ubicacionDestino.filter(InstantaneaUbicacion::almacen).isPresent()
                            && destinoExterno.isEmpty() && presenciaAnterior && presenciaNueva
                        && estadoAnterior.orElse(null) == EstadoEquipo.DISPONIBLE
                        && estadoNuevo.orElse(null) == EstadoEquipo.DISPONIBLE
                        && !mismaSede(ubicacionOrigen.orElseThrow().sede(), ubicacionDestino.orElseThrow().sede()),
                    "Un traslado requiere origen y destino de almacen");
            case CAMBIO_UBICACION -> exigir(ubicacionOrigen.isPresent() && ubicacionDestino.isPresent()
                            && destinoExterno.isEmpty()
                            && presenciaAnterior == ubicacionOrigen.get().almacen()
                            && presenciaNueva == ubicacionDestino.get().almacen()
                        && estadoAnterior.isPresent() && estadoAnterior.equals(estadoNuevo)
                        && estadoNuevo.get() != EstadoEquipo.DE_BAJA
                        && mismaSede(ubicacionOrigen.get().sede(), ubicacionDestino.get().sede()),
                    "Un cambio de ubicacion requiere instantaneas coherentes en la misma sede");
            case CAMBIO_ESTADO -> exigir(destinoExterno.isEmpty()
                            && estadoAnterior.isPresent() && estadoNuevo.isPresent()
                        && estadoAnterior.get() != EstadoEquipo.DE_BAJA
                        && estadoAnterior.get() != EstadoEquipo.ANULADO
                        && estadoAnterior.get() != estadoNuevo.get() && presenciaAnterior == presenciaNueva
                        && estadoNuevo.get() != EstadoEquipo.ANULADO
                        && (presenciaAnterior || estadoNuevo.get() != EstadoEquipo.DISPONIBLE)
                        && ubicacionEstadoCoherente(),
                    "Un cambio de estado requiere estados distintos y no puede cambiar presencia");
            case ANULACION_ALTA -> exigir(ubicacionOrigen.filter(InstantaneaUbicacion::almacen).isPresent()
                            && ubicacionDestino.isEmpty() && destinoExterno.isEmpty()
                            && estadoAnterior.orElse(null) == EstadoEquipo.DISPONIBLE
                            && estadoNuevo.orElse(null) == EstadoEquipo.ANULADO
                            && presenciaAnterior && !presenciaNueva,
                    "La anulacion de alta requiere un ingreso inicial disponible en almacen");
        }
    }

    private boolean ubicacionEstadoCoherente() {
        if (ubicacionOrigen.isEmpty() && ubicacionDestino.isEmpty()) {
            return true;
        }
        return ubicacionOrigen.isPresent() && ubicacionOrigen.equals(ubicacionDestino)
                && presenciaAnterior == ubicacionOrigen.get().almacen();
    }

            private boolean mismaSede(String sedeOrigen, String sedeDestino) {
            return sedeOrigen.strip().equalsIgnoreCase(sedeDestino.strip());
            }

    private void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    public record InstantaneaEquipo(UUID id, UUID categoriaId, String codigo, String numeroSerie,
                                    String marca, String modelo,
                                    String nombreCategoria, String nombreProveedor) {
        public InstantaneaEquipo {
            id = ValidacionDominio.identificador(id, "idEquipo");
            categoriaId = ValidacionDominio.identificador(categoriaId, "categoriaId");
            codigo = ValidacionDominio.textoObligatorio(codigo, "codigo");
            numeroSerie = ValidacionDominio.textoObligatorio(numeroSerie, "numeroSerie");
            marca = ValidacionDominio.textoObligatorio(marca, "marca");
            modelo = ValidacionDominio.textoObligatorio(modelo, "modelo");
            nombreCategoria = ValidacionDominio.textoObligatorio(nombreCategoria, "nombreCategoria");
            nombreProveedor = ValidacionDominio.textoObligatorio(nombreProveedor, "nombreProveedor");
        }
    }

    public record InstantaneaUbicacion(UUID id, String sede, String ambiente, String area, String piso,
                                       boolean almacen) {
        public InstantaneaUbicacion {
            id = ValidacionDominio.identificador(id, "idUbicacion");
            sede = ValidacionDominio.textoObligatorio(sede, "sede");
            ambiente = ValidacionDominio.textoObligatorio(ambiente, "ambiente");
            area = ValidacionDominio.textoObligatorio(area, "area");
            piso = ValidacionDominio.textoObligatorio(piso, "piso");
        }
    }
}