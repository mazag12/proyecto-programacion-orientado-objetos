package com.computototal.inventario.dao.memoria;

import com.computototal.inventario.dao.ConfiguracionStockMinimoDAO;
import com.computototal.inventario.modelo.ConfiguracionStockMinimo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ConfiguracionStockMinimoDAOMemoria implements ConfiguracionStockMinimoDAO {
    private final Map<UUID, ConfiguracionStockMinimo> configuraciones = new LinkedHashMap<>();

    @Override
    public void guardar(ConfiguracionStockMinimo configuracion) {
        ConfiguracionStockMinimo validada = ValidacionDAO.entidad(configuracion, "configuracion");
        UUID id = ValidacionDAO.id(validada.getId());
        if (configuraciones.containsKey(id)) {
            throw ValidacionDAO.duplicado("identificador de configuracion", id.toString());
        }
        validarCombinacion(validada, null);
        ConfiguracionStockMinimo copia = CopiasModelo.copiar(validada);
        configuraciones.put(id, copia);
    }

    @Override
    public Optional<ConfiguracionStockMinimo> buscarPorId(UUID id) {
        return Optional.ofNullable(configuraciones.get(ValidacionDAO.id(id))).map(CopiasModelo::copiar);
    }

    @Override
    public Optional<ConfiguracionStockMinimo> buscarPorCategoriaYSede(UUID categoriaId, String sede) {
        UUID idCategoria = ValidacionDAO.id(categoriaId);
        String claveSede = ValidacionDAO.textoClave(sede, "sede");
        return configuraciones.values().stream()
                .filter(configuracion -> configuracion.getCategoriaId().equals(idCategoria)
                        && ValidacionDAO.textoClave(configuracion.getSede(), "sede").equals(claveSede))
                .findFirst()
                .map(CopiasModelo::copiar);
    }

    @Override
    public List<ConfiguracionStockMinimo> listar() {
        List<ConfiguracionStockMinimo> resultado = new ArrayList<>(configuraciones.size());
        configuraciones.values().forEach(configuracion -> resultado.add(CopiasModelo.copiar(configuracion)));
        return List.copyOf(resultado);
    }

    @Override
    public void actualizar(ConfiguracionStockMinimo configuracion) {
        ConfiguracionStockMinimo validada = ValidacionDAO.entidad(configuracion, "configuracion");
        UUID id = ValidacionDAO.id(validada.getId());
        if (!configuraciones.containsKey(id)) {
            throw ValidacionDAO.inexistente("configuracion de stock minimo", id);
        }
        validarCombinacion(validada, id);
        ConfiguracionStockMinimo copia = CopiasModelo.copiar(validada);
        configuraciones.put(id, copia);
    }

    @Override
    public void eliminar(UUID id) {
        UUID idValidado = ValidacionDAO.id(id);
        if (configuraciones.remove(idValidado) == null) {
            throw ValidacionDAO.inexistente("configuracion de stock minimo", idValidado);
        }
    }

    private void validarCombinacion(ConfiguracionStockMinimo candidato, UUID idExcluido) {
        String sede = ValidacionDAO.textoClave(candidato.getSede(), "sede");
        for (ConfiguracionStockMinimo existente : configuraciones.values()) {
            if (existente.getId().equals(idExcluido)) {
                continue;
            }
            boolean mismaCategoria = existente.getCategoriaId().equals(candidato.getCategoriaId());
            boolean mismaSede = ValidacionDAO.textoClave(existente.getSede(), "sede").equals(sede);
            if (mismaCategoria && mismaSede) {
                throw ValidacionDAO.duplicado("configuracion para categoria y sede", sede);
            }
        }
    }
}