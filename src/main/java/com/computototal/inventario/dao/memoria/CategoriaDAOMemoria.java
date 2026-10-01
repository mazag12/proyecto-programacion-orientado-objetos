package com.computototal.inventario.dao.memoria;

import com.computototal.inventario.dao.CategoriaDAO;
import com.computototal.inventario.modelo.Categoria;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CategoriaDAOMemoria implements CategoriaDAO {
    private final Map<UUID, Categoria> categorias = new LinkedHashMap<>();

    @Override
    public void insertar(Categoria categoria) {
        Categoria validada = ValidacionDAO.entidad(categoria, "categoria");
        UUID id = ValidacionDAO.id(validada.getId());
        if (categorias.containsKey(id)) {
            throw ValidacionDAO.duplicado("identificador de categoria", id.toString());
        }
        Categoria copia = CopiasModelo.copiar(validada);
        categorias.put(id, copia);
    }

    @Override
    public Optional<Categoria> buscarPorId(UUID id) {
        return Optional.ofNullable(categorias.get(ValidacionDAO.id(id))).map(CopiasModelo::copiar);
    }

    @Override
    public List<Categoria> listar() {
        List<Categoria> resultado = new ArrayList<>(categorias.size());
        categorias.values().forEach(categoria -> resultado.add(CopiasModelo.copiar(categoria)));
        return List.copyOf(resultado);
    }

    @Override
    public void actualizar(Categoria categoria) {
        Categoria validada = ValidacionDAO.entidad(categoria, "categoria");
        UUID id = ValidacionDAO.id(validada.getId());
        if (!categorias.containsKey(id)) {
            throw ValidacionDAO.inexistente("categoria", id);
        }
        Categoria copia = CopiasModelo.copiar(validada);
        categorias.put(id, copia);
    }

    @Override
    public void eliminar(UUID id) {
        UUID idValidado = ValidacionDAO.id(id);
        if (categorias.remove(idValidado) == null) {
            throw ValidacionDAO.inexistente("categoria", idValidado);
        }
    }
}