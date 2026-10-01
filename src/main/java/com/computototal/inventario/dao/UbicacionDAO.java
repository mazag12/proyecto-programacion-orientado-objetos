package com.computototal.inventario.dao;

import com.computototal.inventario.modelo.Ubicacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UbicacionDAO {
    void insertar(Ubicacion ubicacion);

    Optional<Ubicacion> buscarPorId(UUID id);

    List<Ubicacion> listar();

    void actualizar(Ubicacion ubicacion);

    void eliminar(UUID id);
}