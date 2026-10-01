package com.computototal.inventario.dao;

import com.computototal.inventario.modelo.ConfiguracionStockMinimo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfiguracionStockMinimoDAO {
    void guardar(ConfiguracionStockMinimo configuracion);

    Optional<ConfiguracionStockMinimo> buscarPorId(UUID id);

    Optional<ConfiguracionStockMinimo> buscarPorCategoriaYSede(UUID categoriaId, String sede);

    List<ConfiguracionStockMinimo> listar();

    void actualizar(ConfiguracionStockMinimo configuracion);

    void eliminar(UUID id);
}