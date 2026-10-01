package com.computototal.inventario.dao.memoria;

import com.computototal.inventario.modelo.Categoria;
import com.computototal.inventario.modelo.ConfiguracionStockMinimo;
import com.computototal.inventario.modelo.Equipo;
import com.computototal.inventario.modelo.Movimiento;
import com.computototal.inventario.modelo.Proveedor;
import com.computototal.inventario.modelo.Ubicacion;
import com.computototal.inventario.modelo.Usuario;

final class CopiasModelo {
    private CopiasModelo() {
    }

    static Equipo copiar(Equipo equipo) {
        return ValidacionDAO.entidad(equipo, "equipo").copiar();
    }

    static Categoria copiar(Categoria categoria) {
        Categoria origen = ValidacionDAO.entidad(categoria, "categoria");
        return new Categoria(origen.getId(), origen.getNombre(), origen.getDescripcion());
    }

    static Proveedor copiar(Proveedor proveedor) {
        Proveedor origen = ValidacionDAO.entidad(proveedor, "proveedor");
        return new Proveedor(origen.getId(), origen.getNombre(), origen.getTelefono(), origen.getCorreoElectronico());
    }

    static Ubicacion copiar(Ubicacion ubicacion) {
        Ubicacion origen = ValidacionDAO.entidad(ubicacion, "ubicacion");
        return new Ubicacion(origen.getId(), origen.getSede(), origen.getAmbiente(), origen.getArea(),
                origen.getPiso(), origen.isAlmacen());
    }

    static Usuario copiar(Usuario usuario) {
        Usuario origen = ValidacionDAO.entidad(usuario, "usuario");
        return new Usuario(origen.getId(), origen.getNombreUsuario(), origen.getHashContrasena(),
                origen.getSalContrasena(), origen.getRol());
    }

    static ConfiguracionStockMinimo copiar(ConfiguracionStockMinimo configuracion) {
        ConfiguracionStockMinimo origen = ValidacionDAO.entidad(configuracion, "configuracion");
        return new ConfiguracionStockMinimo(origen.getId(), origen.getCategoriaId(), origen.getSede(),
                origen.getMinimo());
    }

    static Movimiento copiar(Movimiento movimiento) {
        return ValidacionDAO.entidad(movimiento, "movimiento");
    }
}