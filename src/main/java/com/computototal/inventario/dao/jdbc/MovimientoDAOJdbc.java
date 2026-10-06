package com.computototal.inventario.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.computototal.inventario.dao.MovimientoDAO;
import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.Movimiento;
import com.computototal.inventario.modelo.TipoMovimiento;

public final class MovimientoDAOJdbc extends JdbcDAO implements MovimientoDAO {
    private static final String COLUMNAS = "id, fecha_hora, tipo, motivo, equipo_id, equipo_categoria_id, "
            + "equipo_codigo, equipo_numero_serie, equipo_marca, equipo_modelo, categoria_nombre, "
            + "proveedor_nombre, responsable_id, responsable_nombre, origen_id, origen_sede, origen_ambiente, "
            + "origen_area, origen_piso, origen_es_almacen, destino_id, destino_sede, destino_ambiente, "
            + "destino_area, destino_piso, destino_es_almacen, destino_externo, estado_anterior, estado_nuevo, "
            + "presencia_anterior, presencia_nueva";
    private static final String INSERTAR = "INSERT INTO movimiento (" + COLUMNAS + ") VALUES ("
            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public MovimientoDAOJdbc(HsqlDatabase base) {
        super(base);
    }

    @Override
    public void insertar(Movimiento movimiento) {
        Movimiento valor = Objects.requireNonNull(movimiento, "movimiento");
        base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(INSERTAR)) {
                int indice = 1;
                ponerId(sentencia, indice++, valor.getId());
                sentencia.setTimestamp(indice++, Timestamp.valueOf(valor.getFechaHora()));
                sentencia.setString(indice++, valor.getTipo().name());
                sentencia.setString(indice++, valor.getMotivo());
                Movimiento.InstantaneaEquipo equipo = valor.getEquipo();
                ponerId(sentencia, indice++, equipo.id());
                ponerId(sentencia, indice++, equipo.categoriaId());
                sentencia.setString(indice++, equipo.codigo());
                sentencia.setString(indice++, equipo.numeroSerie());
                sentencia.setString(indice++, equipo.marca());
                sentencia.setString(indice++, equipo.modelo());
                sentencia.setString(indice++, equipo.nombreCategoria());
                sentencia.setString(indice++, equipo.nombreProveedor());
                ponerId(sentencia, indice++, valor.getResponsableId());
                sentencia.setString(indice++, valor.getNombreResponsable());
                indice = escribirUbicacion(sentencia, indice, valor.getUbicacionOrigen());
                indice = escribirUbicacion(sentencia, indice, valor.getUbicacionDestino());
                ponerTextoOpcional(sentencia, indice++, valor.getDestinoExterno());
                ponerEstado(sentencia, indice++, valor.getEstadoAnterior());
                ponerEstado(sentencia, indice++, valor.getEstadoNuevo());
                sentencia.setBoolean(indice++, valor.isPresenciaAnterior());
                sentencia.setBoolean(indice, valor.isPresenciaNueva());
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<Movimiento> buscarPorId(UUID id) {
        Objects.requireNonNull(id, "id");
        return base.ejecutar(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM movimiento WHERE id = ?")) {
                ponerId(sentencia, 1, id);
                try (ResultSet fila = sentencia.executeQuery()) {
                    return fila.next() ? Optional.of(mapear(fila)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Movimiento> listar() {
        return listarCon("SELECT " + COLUMNAS + " FROM movimiento ORDER BY fecha_hora, id", null);
    }

    @Override
    public List<Movimiento> listarPorEquipo(UUID equipoId) {
        Objects.requireNonNull(equipoId, "equipoId");
        return listarCon("SELECT " + COLUMNAS + " FROM movimiento WHERE equipo_id = ? ORDER BY fecha_hora, id",
                equipoId);
    }

    @Override
    public List<Movimiento> listarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        return base.ejecutar(conexion -> {
            List<Movimiento> resultado = new ArrayList<>();
            String sql = "SELECT " + COLUMNAS + " FROM movimiento WHERE fecha_hora >= ? AND fecha_hora <= ? "
                    + "ORDER BY fecha_hora, id";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setTimestamp(1, Timestamp.valueOf(inicio));
                sentencia.setTimestamp(2, Timestamp.valueOf(fin));
                try (ResultSet filas = sentencia.executeQuery()) {
                    while (filas.next()) {
                        resultado.add(mapear(filas));
                    }
                }
            }
            return List.copyOf(resultado);
        });
    }

    private List<Movimiento> listarCon(String sql, UUID equipoId) {
        return base.ejecutar(conexion -> {
            List<Movimiento> resultado = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                if (equipoId != null) {
                    ponerId(sentencia, 1, equipoId);
                }
                try (ResultSet filas = sentencia.executeQuery()) {
                    while (filas.next()) {
                        resultado.add(mapear(filas));
                    }
                }
            }
            return List.copyOf(resultado);
        });
    }

    private static int escribirUbicacion(PreparedStatement sentencia, int indice,
                                          Optional<Movimiento.InstantaneaUbicacion> ubicacion) throws SQLException {
        if (ubicacion.isEmpty()) {
            for (int campo = 0; campo < 6; campo++) {
                sentencia.setNull(indice++, campo == 5 ? java.sql.Types.BOOLEAN : java.sql.Types.VARCHAR);
            }
            return indice;
        }
        Movimiento.InstantaneaUbicacion valor = ubicacion.orElseThrow();
        ponerId(sentencia, indice++, valor.id());
        sentencia.setString(indice++, valor.sede());
        sentencia.setString(indice++, valor.ambiente());
        sentencia.setString(indice++, valor.area());
        sentencia.setString(indice++, valor.piso());
        sentencia.setBoolean(indice++, valor.almacen());
        return indice;
    }

    private static void ponerEstado(PreparedStatement sentencia, int indice, Optional<EstadoEquipo> estado)
            throws SQLException {
        if (estado.isPresent()) {
            sentencia.setString(indice, estado.orElseThrow().name());
        } else {
            sentencia.setNull(indice, java.sql.Types.VARCHAR);
        }
    }

    private static Movimiento mapear(ResultSet fila) throws SQLException {
        Movimiento.InstantaneaEquipo equipo = new Movimiento.InstantaneaEquipo(leerId(fila, "equipo_id"),
                leerId(fila, "equipo_categoria_id"), leerTexto(fila, "equipo_codigo"),
                leerTexto(fila, "equipo_numero_serie"), leerTexto(fila, "equipo_marca"),
                leerTexto(fila, "equipo_modelo"), leerTexto(fila, "categoria_nombre"),
                leerTexto(fila, "proveedor_nombre"));
        return new Movimiento(leerId(fila, "id"), fila.getTimestamp("fecha_hora").toLocalDateTime(),
                TipoMovimiento.valueOf(leerTexto(fila, "tipo")), leerTexto(fila, "motivo"), equipo,
                leerId(fila, "responsable_id"), leerTexto(fila, "responsable_nombre"),
                leerUbicacion(fila, "origen"), leerUbicacion(fila, "destino"),
                leerTextoOpcional(fila, "destino_externo"), leerEstado(fila, "estado_anterior"),
                leerEstado(fila, "estado_nuevo"), fila.getBoolean("presencia_anterior"),
                fila.getBoolean("presencia_nueva"));
    }

    private static Optional<Movimiento.InstantaneaUbicacion> leerUbicacion(ResultSet fila, String prefijo)
            throws SQLException {
        String id = fila.getString(prefijo + "_id");
        if (id == null) {
            return Optional.empty();
        }
        return Optional.of(new Movimiento.InstantaneaUbicacion(UUID.fromString(id),
                leerTexto(fila, prefijo + "_sede"), leerTexto(fila, prefijo + "_ambiente"),
                leerTexto(fila, prefijo + "_area"), leerTexto(fila, prefijo + "_piso"),
                fila.getBoolean(prefijo + "_es_almacen")));
    }

    private static Optional<EstadoEquipo> leerEstado(ResultSet fila, String columna) throws SQLException {
        String estado = fila.getString(columna);
        return estado == null ? Optional.empty() : Optional.of(EstadoEquipo.valueOf(estado));
    }
}