package com.computototal.inventario.dao.memoria;

import com.computototal.inventario.dao.EquipoDAO;
import com.computototal.inventario.modelo.Equipo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EquipoDAOMemoria implements EquipoDAO {
    private final Map<UUID, Equipo> equipos = new LinkedHashMap<>();

    @Override
    public void insertar(Equipo equipo) {
        Equipo validado = ValidacionDAO.entidad(equipo, "equipo");
        UUID id = ValidacionDAO.id(validado.getId());
        if (equipos.containsKey(id)) {
            throw ValidacionDAO.duplicado("identificador de equipo", id.toString());
        }
        validarCodigoYSerie(validado, null);
        Equipo copia = CopiasModelo.copiar(validado);
        equipos.put(id, copia);
    }

    @Override
    public Optional<Equipo> buscarPorId(UUID id) {
        Equipo equipo = equipos.get(ValidacionDAO.id(id));
        return Optional.ofNullable(equipo).map(CopiasModelo::copiar);
    }

    @Override
    public Optional<Equipo> buscarPorCodigo(String codigo) {
        String clave = ValidacionDAO.textoClave(codigo, "codigo");
        return equipos.values().stream()
                .filter(equipo -> ValidacionDAO.textoClave(equipo.getCodigo(), "codigo").equals(clave))
                .findFirst()
                .map(CopiasModelo::copiar);
    }

    @Override
    public Optional<Equipo> buscarPorNumeroSerie(String numeroSerie) {
        String clave = ValidacionDAO.textoClave(numeroSerie, "numeroSerie");
        return equipos.values().stream()
                .filter(equipo -> ValidacionDAO.textoClave(equipo.getNumeroSerie(), "numeroSerie").equals(clave))
                .findFirst()
                .map(CopiasModelo::copiar);
    }

    @Override
    public List<Equipo> listar() {
        List<Equipo> resultado = new ArrayList<>(equipos.size());
        equipos.values().forEach(equipo -> resultado.add(CopiasModelo.copiar(equipo)));
        return List.copyOf(resultado);
    }

    @Override
    public void actualizar(Equipo equipo) {
        Equipo validado = ValidacionDAO.entidad(equipo, "equipo");
        UUID id = ValidacionDAO.id(validado.getId());
        if (!equipos.containsKey(id)) {
            throw ValidacionDAO.inexistente("equipo", id);
        }
        validarCodigoYSerie(validado, id);
        Equipo copia = CopiasModelo.copiar(validado);
        equipos.put(id, copia);
    }

    @Override
    public void eliminar(UUID id) {
        UUID idValidado = ValidacionDAO.id(id);
        if (equipos.remove(idValidado) == null) {
            throw ValidacionDAO.inexistente("equipo", idValidado);
        }
    }

    private void validarCodigoYSerie(Equipo candidato, UUID idExcluido) {
        String codigo = ValidacionDAO.textoClave(candidato.getCodigo(), "codigo");
        String serie = ValidacionDAO.textoClave(candidato.getNumeroSerie(), "numeroSerie");
        for (Equipo existente : equipos.values()) {
            if (existente.getId().equals(idExcluido)) {
                continue;
            }
            if (ValidacionDAO.textoClave(existente.getCodigo(), "codigo").equals(codigo)) {
                throw ValidacionDAO.duplicado("codigo de equipo", candidato.getCodigo());
            }
            if (ValidacionDAO.textoClave(existente.getNumeroSerie(), "numeroSerie").equals(serie)) {
                throw ValidacionDAO.duplicado("numero de serie", candidato.getNumeroSerie());
            }
        }
    }
}