package com.computototal.inventario.dao.memoria;

import com.computototal.inventario.dao.ProveedorDAO;
import com.computototal.inventario.modelo.Proveedor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ProveedorDAOMemoria implements ProveedorDAO {
    private final Map<UUID, Proveedor> proveedores = new LinkedHashMap<>();

    @Override
    public void insertar(Proveedor proveedor) {
        Proveedor validado = ValidacionDAO.entidad(proveedor, "proveedor");
        UUID id = ValidacionDAO.id(validado.getId());
        if (proveedores.containsKey(id)) {
            throw ValidacionDAO.duplicado("identificador de proveedor", id.toString());
        }
        Proveedor copia = CopiasModelo.copiar(validado);
        proveedores.put(id, copia);
    }

    @Override
    public Optional<Proveedor> buscarPorId(UUID id) {
        return Optional.ofNullable(proveedores.get(ValidacionDAO.id(id))).map(CopiasModelo::copiar);
    }

    @Override
    public List<Proveedor> listar() {
        List<Proveedor> resultado = new ArrayList<>(proveedores.size());
        proveedores.values().forEach(proveedor -> resultado.add(CopiasModelo.copiar(proveedor)));
        return List.copyOf(resultado);
    }

    @Override
    public void actualizar(Proveedor proveedor) {
        Proveedor validado = ValidacionDAO.entidad(proveedor, "proveedor");
        UUID id = ValidacionDAO.id(validado.getId());
        if (!proveedores.containsKey(id)) {
            throw ValidacionDAO.inexistente("proveedor", id);
        }
        Proveedor copia = CopiasModelo.copiar(validado);
        proveedores.put(id, copia);
    }

    @Override
    public void eliminar(UUID id) {
        UUID idValidado = ValidacionDAO.id(id);
        if (proveedores.remove(idValidado) == null) {
            throw ValidacionDAO.inexistente("proveedor", idValidado);
        }
    }
}