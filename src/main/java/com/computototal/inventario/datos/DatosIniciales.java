package com.computototal.inventario.datos;

import java.util.Objects;
import java.util.UUID;

import com.computototal.inventario.dao.CategoriaDAO;
import com.computototal.inventario.dao.ProveedorDAO;
import com.computototal.inventario.dao.UbicacionDAO;
import com.computototal.inventario.dao.UsuarioDAO;
import com.computototal.inventario.modelo.Categoria;
import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.Proveedor;
import com.computototal.inventario.modelo.Rol;
import com.computototal.inventario.modelo.Ubicacion;
import com.computototal.inventario.modelo.Usuario;
import com.computototal.inventario.servicio.AutenticacionServicio;
import com.computototal.inventario.servicio.EquipoServicio;
import com.computototal.inventario.servicio.GestorContrasenas;
import com.computototal.inventario.servicio.MovimientoServicio;
import com.computototal.inventario.servicio.StockServicio;

public final class DatosIniciales {
    public static final String USUARIO_ADMIN = "admin";
    public static final String CLAVE_ADMIN = "admin-demo-2026";
    public static final String USUARIO_ALMACENERO = "almacen";
    public static final String CLAVE_ALMACENERO = "almacen-demo-2026";
    public static final String USUARIO_AUDITOR = "auditor";
    public static final String CLAVE_AUDITOR = "auditor-demo-2026";

    private final CategoriaDAO categoriaDAO;
    private final ProveedorDAO proveedorDAO;
    private final UbicacionDAO ubicacionDAO;
    private final UsuarioDAO usuarioDAO;
    private final GestorContrasenas gestorContrasenas;
    private final AutenticacionServicio autenticacionServicio;
    private final EquipoServicio equipoServicio;
    private final MovimientoServicio movimientoServicio;
    private final StockServicio stockServicio;
    private boolean cargado;

    public DatosIniciales(CategoriaDAO categoriaDAO, ProveedorDAO proveedorDAO, UbicacionDAO ubicacionDAO,
                          UsuarioDAO usuarioDAO, GestorContrasenas gestorContrasenas,
                          AutenticacionServicio autenticacionServicio, EquipoServicio equipoServicio,
                          MovimientoServicio movimientoServicio, StockServicio stockServicio) {
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO");
        this.proveedorDAO = Objects.requireNonNull(proveedorDAO, "proveedorDAO");
        this.ubicacionDAO = Objects.requireNonNull(ubicacionDAO, "ubicacionDAO");
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO");
        this.gestorContrasenas = Objects.requireNonNull(gestorContrasenas, "gestorContrasenas");
        this.autenticacionServicio = Objects.requireNonNull(autenticacionServicio, "autenticacionServicio");
        this.equipoServicio = Objects.requireNonNull(equipoServicio, "equipoServicio");
        this.movimientoServicio = Objects.requireNonNull(movimientoServicio, "movimientoServicio");
        this.stockServicio = Objects.requireNonNull(stockServicio, "stockServicio");
    }

    public synchronized void cargar() {
        if (cargado) {
            return;
        }
        cargarCatalogos();
        cargarUsuarios();
        autenticacionServicio.iniciarSesion(USUARIO_ADMIN, CLAVE_ADMIN.toCharArray());
        try {
            cargarInventario();
        } finally {
            autenticacionServicio.cerrarSesion();
        }
        cargado = true;
    }

    private void cargarCatalogos() {
        categoriaDAO.insertar(new Categoria(id(1), "Laptop", "Equipos portatiles"));
        categoriaDAO.insertar(new Categoria(id(2), "Desktop", "Equipos de escritorio"));
        categoriaDAO.insertar(new Categoria(id(3), "Impresora", "Impresoras de oficina"));

        proveedorDAO.insertar(new Proveedor(id(11), "Proveedor Andino", "999111222", "ventas@andino.example"));
        proveedorDAO.insertar(new Proveedor(id(12), "Proveedor Pacifico", "999333444", "ventas@pacifico.example"));

        ubicacionDAO.insertar(new Ubicacion(id(21), "Lima", "Almacen principal", "Inventario", "PB", true));
        ubicacionDAO.insertar(new Ubicacion(id(22), "Lima", "Almacen secundario", "Inventario", "Sotano", true));
        ubicacionDAO.insertar(new Ubicacion(id(23), "Lima", "Soporte", "Operaciones", "2", false));
        ubicacionDAO.insertar(new Ubicacion(id(24), "Arequipa", "Almacen regional", "Inventario", "PB", true));
        ubicacionDAO.insertar(new Ubicacion(id(25), "Arequipa", "Administracion", "Operaciones", "1", false));
    }

    private void cargarUsuarios() {
        insertarUsuario(id(31), USUARIO_ADMIN, CLAVE_ADMIN, Rol.ADMINISTRADOR);
        insertarUsuario(id(32), USUARIO_ALMACENERO, CLAVE_ALMACENERO, Rol.ALMACENERO);
        insertarUsuario(id(33), USUARIO_AUDITOR, CLAVE_AUDITOR, Rol.AUDITOR);
    }

    private void insertarUsuario(UUID id, String nombre, String clave, Rol rol) {
        GestorContrasenas.Credencial credencial = gestorContrasenas.generarHash(clave.toCharArray());
        usuarioDAO.insertar(new Usuario(id, nombre, credencial.getHashContrasena(),
                credencial.getSalContrasena(), rol));
    }

    private void cargarInventario() {
        equipoServicio.registrarEquipoConIngreso(id(101), "CT-1001", "GMD-LAP-1001",
                "Lenovo", "ThinkPad T14", id(1), id(11), id(21), "Ingreso de apertura");
        equipoServicio.registrarEquipoConIngreso(id(102), "CT-1002", "GMD-LAP-1002",
                "Dell", "Latitude 5440", id(1), id(11), id(21), "Ingreso de apertura");
        equipoServicio.registrarEquipoConIngreso(id(103), "CT-1003", "GMD-DES-1003",
                "HP", "ProDesk 400", id(2), id(12), id(24), "Ingreso de apertura");
        equipoServicio.registrarEquipoConIngreso(id(104), "CT-1004", "GMD-IMP-1004",
                "Epson", "EcoTank L6270", id(3), id(12), id(21), "Ingreso de apertura");
        equipoServicio.registrarEquipoConIngreso(id(105), "CT-1005", "GMD-LAP-1005",
                "HP", "EliteBook 840", id(1), id(11), id(21), "Ingreso de apertura");

        movimientoServicio.cambiarEstado(id(104), EstadoEquipo.EN_MANTENIMIENTO, "Mantenimiento preventivo");
        movimientoServicio.registrarSalida(id(105), "Puesto de soporte Lima", "Asignacion de usuario");
        movimientoServicio.trasladar(id(101), id(24), "Reubicacion inicial de sede");
        stockServicio.configurarMinimo(id(1), "Lima", 3);
        stockServicio.configurarMinimo(id(2), "Arequipa", 1);
        stockServicio.configurarMinimo(id(3), "Lima", 0);
        stockServicio.configurarMinimo(id(1), "Arequipa", 1);
    }

    private UUID id(long valor) {
        return new UUID(0L, valor);
    }
}