package com.computototal.inventario.dao;

import com.computototal.inventario.modelo.Proveedor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProveedorDAO {
    void insertar(Proveedor proveedor);

    Optional<Proveedor> buscarPorId(UUID id);

    List<Proveedor> listar();

    void actualizar(Proveedor proveedor);

    void eliminar(UUID id);
}