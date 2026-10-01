package com.computototal.inventario.dao;

import com.computototal.inventario.modelo.Equipo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipoDAO {
    void insertar(Equipo equipo);

    Optional<Equipo> buscarPorId(UUID id);

    Optional<Equipo> buscarPorCodigo(String codigo);

    Optional<Equipo> buscarPorNumeroSerie(String numeroSerie);

    List<Equipo> listar();

    void actualizar(Equipo equipo);

    void eliminar(UUID id);
}