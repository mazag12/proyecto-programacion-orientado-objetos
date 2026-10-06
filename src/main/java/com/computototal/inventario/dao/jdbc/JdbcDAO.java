package com.computototal.inventario.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.computototal.inventario.validacion.ValidacionesEntrada;

abstract class JdbcDAO {
    protected final HsqlDatabase base;

    JdbcDAO(HsqlDatabase base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    protected static String clave(String valor, String campo) {
        return ValidacionesEntrada.textoClave(valor, campo);
    }

    protected static String leerTexto(ResultSet fila, String columna) throws SQLException {
        return fila.getString(columna);
    }

    protected static UUID leerId(ResultSet fila, String columna) throws SQLException {
        return UUID.fromString(fila.getString(columna));
    }

    protected static Optional<UUID> leerIdOpcional(ResultSet fila, String columna) throws SQLException {
        String valor = fila.getString(columna);
        return valor == null ? Optional.empty() : Optional.of(UUID.fromString(valor));
    }

    protected static Optional<String> leerTextoOpcional(ResultSet fila, String columna) throws SQLException {
        return Optional.ofNullable(fila.getString(columna));
    }

    protected static void ponerId(PreparedStatement sentencia, int indice, UUID id) throws SQLException {
        sentencia.setString(indice, id.toString());
    }

    protected static void ponerIdOpcional(PreparedStatement sentencia, int indice, Optional<UUID> id)
            throws SQLException {
        if (id.isPresent()) {
            ponerId(sentencia, indice, id.orElseThrow());
        } else {
            sentencia.setNull(indice, java.sql.Types.VARCHAR);
        }
    }

    protected static void ponerTextoOpcional(PreparedStatement sentencia, int indice, Optional<String> texto)
            throws SQLException {
        if (texto.isPresent()) {
            sentencia.setString(indice, texto.orElseThrow());
        } else {
            sentencia.setNull(indice, java.sql.Types.VARCHAR);
        }
    }

    protected static void exigirActualizado(int filas, String entidad, UUID id) {
        if (filas == 0) {
            throw new NoSuchElementException("No existe " + entidad + " con identificador " + id);
        }
    }
}