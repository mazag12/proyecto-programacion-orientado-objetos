package com.computototal.inventario.dao;

import com.computototal.inventario.modelo.Categoria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaDAO {
    void insertar(Categoria categoria);

    Optional<Categoria> buscarPorId(UUID id);

    List<Categoria> listar();

    void actualizar(Categoria categoria);

    void eliminar(UUID id);
}