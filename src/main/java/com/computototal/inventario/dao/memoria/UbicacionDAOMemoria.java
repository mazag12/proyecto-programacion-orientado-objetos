package com.computototal.inventario.dao.memoria;

import com.computototal.inventario.dao.UbicacionDAO;
import com.computototal.inventario.modelo.Ubicacion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UbicacionDAOMemoria implements UbicacionDAO {
    private final Map<UUID, Ubicacion> ubicaciones = new LinkedHashMap<>();

    @Override
    public void insertar(Ubicacion ubicacion) {
        Ubicacion validada = ValidacionDAO.entidad(ubicacion, "ubicacion");
        UUID id = ValidacionDAO.id(validada.getId());
        if (ubicaciones.containsKey(id)) {
            throw ValidacionDAO.duplicado("identificador de ubicacion", id.toString());
        }
        Ubicacion copia = CopiasModelo.copiar(validada);
        ubicaciones.put(id, copia);
    }

    @Override
    public Optional<Ubicacion> buscarPorId(UUID id) {
        return Optional.ofNullable(ubicaciones.get(ValidacionDAO.id(id))).map(CopiasModelo::copiar);
    }

    @Override
    public List<Ubicacion> listar() {
        List<Ubicacion> resultado = new ArrayList<>(ubicaciones.size());
        ubicaciones.values().forEach(ubicacion -> resultado.add(CopiasModelo.copiar(ubicacion)));
        return List.copyOf(resultado);
    }

    @Override
    public void actualizar(Ubicacion ubicacion) {
        Ubicacion validada = ValidacionDAO.entidad(ubicacion, "ubicacion");
        UUID id = ValidacionDAO.id(validada.getId());
        if (!ubicaciones.containsKey(id)) {
            throw ValidacionDAO.inexistente("ubicacion", id);
        }
        Ubicacion copia = CopiasModelo.copiar(validada);
        ubicaciones.put(id, copia);
    }

    @Override
    public void eliminar(UUID id) {
        UUID idValidado = ValidacionDAO.id(id);
        if (ubicaciones.remove(idValidado) == null) {
            throw ValidacionDAO.inexistente("ubicacion", idValidado);
        }
    }
}