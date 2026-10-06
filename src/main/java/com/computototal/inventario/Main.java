package com.computototal.inventario;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.computototal.inventario.dao.CategoriaDAO;
import com.computototal.inventario.dao.ConfiguracionStockMinimoDAO;
import com.computototal.inventario.dao.EquipoDAO;
import com.computototal.inventario.dao.MovimientoDAO;
import com.computototal.inventario.dao.ProveedorDAO;
import com.computototal.inventario.dao.UbicacionDAO;
import com.computototal.inventario.dao.UsuarioDAO;
import com.computototal.inventario.dao.jdbc.CategoriaDAOJdbc;
import com.computototal.inventario.dao.jdbc.ConfiguracionStockMinimoDAOJdbc;
import com.computototal.inventario.dao.jdbc.EquipoDAOJdbc;
import com.computototal.inventario.dao.jdbc.HsqlDatabase;
import com.computototal.inventario.dao.jdbc.MovimientoDAOJdbc;
import com.computototal.inventario.dao.jdbc.ProveedorDAOJdbc;
import com.computototal.inventario.dao.jdbc.UbicacionDAOJdbc;
import com.computototal.inventario.dao.jdbc.UsuarioDAOJdbc;
import com.computototal.inventario.datos.DatosIniciales;
import com.computototal.inventario.demo.DemostracionRF;
import com.computototal.inventario.gui.DashboardInventarioGUI;
import com.computototal.inventario.gui.InicioSesionGUI;
import com.computototal.inventario.servicio.AutenticacionServicio;
import com.computototal.inventario.servicio.AutorizacionServicio;
import com.computototal.inventario.servicio.EquipoServicio;
import com.computototal.inventario.servicio.GestorContrasenas;
import com.computototal.inventario.servicio.MovimientoServicio;
import com.computototal.inventario.servicio.ReporteServicio;
import com.computototal.inventario.servicio.Sesion;
import com.computototal.inventario.servicio.StockServicio;

public final class Main {
    private Main() {
    }

    public static void main(String[] args)  throws Exception {
        boolean demo = args.length == 1 && "--demo".equals(args[0]);
        if (args.length > 0 && !demo) {
            System.err.println("Uso: java -cp target/classes com.computototal.inventario.Main [--demo]");
            System.exit(2);
        }

        String url = demo
                ? "jdbc:hsqldb:mem:inventario_demo"
                : "jdbc:hsqldb:file:./data/mi_base";
                HsqlDatabase base = HsqlDatabase.abrir(url, "SA", "");
                try {
            System.out.println("Conexión JDBC con HSQLDB establecida.");
                        if (demo) {
                                try (base) {
                                        ejecutarAplicacion(true, base);
                                }
                        } else {
                                ejecutarAplicacion(false, base);
                        }
        } catch (java.sql.SQLException e) {
            System.err.println("No se pudo conectar con HSQLDB: " + e.getMessage());
            throw e;
        }
    }

    private static void ejecutarAplicacion(boolean demo, HsqlDatabase base) {
        Clock reloj = demo
                ? Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneId.of("UTC"))
                : Clock.systemDefaultZone();
        Componentes componentes = crearComponentes(reloj, base);
        new DatosIniciales(componentes.categoriaDAO(), componentes.proveedorDAO(), componentes.ubicacionDAO(),
                componentes.usuarioDAO(), componentes.gestorContrasenas(), componentes.autenticacionServicio(),
                componentes.equipoServicio(), componentes.movimientoServicio(), componentes.stockServicio()).cargar();

        if (demo) {
            new DemostracionRF(componentes.autenticacionServicio(), componentes.autorizacionServicio(),
                    componentes.equipoServicio(), componentes.movimientoServicio(), componentes.stockServicio(),
                    componentes.reporteServicio()).ejecutar();
            return;
        }
        if (!InicioSesionGUI.mostrar(componentes.autenticacionServicio())) {
            return;
        }
        new DashboardInventarioGUI(componentes.autenticacionServicio(), componentes.autorizacionServicio(),
                componentes.equipoServicio(), componentes.movimientoServicio(), componentes.stockServicio(),
                componentes.reporteServicio()).mostrar();
    }

    private static Componentes crearComponentes(Clock reloj, HsqlDatabase base) {
        EquipoDAO equipoDAO = new EquipoDAOJdbc(base);
        MovimientoDAO movimientoDAO = new MovimientoDAOJdbc(base);
        CategoriaDAO categoriaDAO = new CategoriaDAOJdbc(base);
        ConfiguracionStockMinimoDAO configuracionDAO = new ConfiguracionStockMinimoDAOJdbc(base);
        ProveedorDAO proveedorDAO = new ProveedorDAOJdbc(base);
        UbicacionDAO ubicacionDAO = new UbicacionDAOJdbc(base);
        UsuarioDAO usuarioDAO = new UsuarioDAOJdbc(base);

        Sesion sesion = new Sesion();
        GestorContrasenas gestorContrasenas = new GestorContrasenas();
        AutenticacionServicio autenticacionServicio = new AutenticacionServicio(usuarioDAO, gestorContrasenas, sesion);
        AutorizacionServicio autorizacionServicio = new AutorizacionServicio(sesion);
        StockServicio stockServicio = new StockServicio(categoriaDAO, configuracionDAO, equipoDAO,
                ubicacionDAO, sesion, autorizacionServicio);
        MovimientoServicio movimientoServicio = new MovimientoServicio(equipoDAO, movimientoDAO,
                categoriaDAO, proveedorDAO, ubicacionDAO, sesion, autorizacionServicio, stockServicio, reloj);
        EquipoServicio equipoServicio = new EquipoServicio(equipoDAO, categoriaDAO, proveedorDAO, ubicacionDAO,
                movimientoDAO, movimientoServicio, sesion, autorizacionServicio);
        ReporteServicio reporteServicio = new ReporteServicio(movimientoDAO, stockServicio, sesion, autorizacionServicio);

        return new Componentes(equipoDAO, movimientoDAO, categoriaDAO, configuracionDAO,
                proveedorDAO, ubicacionDAO, usuarioDAO, gestorContrasenas, autenticacionServicio,
                autorizacionServicio, equipoServicio, movimientoServicio, stockServicio, reporteServicio);
    }

    private record Componentes(
            EquipoDAO equipoDAO,
            MovimientoDAO movimientoDAO,
            CategoriaDAO categoriaDAO,
            ConfiguracionStockMinimoDAO configuracionDAO,
            ProveedorDAO proveedorDAO,
            UbicacionDAO ubicacionDAO,
            UsuarioDAO usuarioDAO,
            GestorContrasenas gestorContrasenas,
            AutenticacionServicio autenticacionServicio,
            AutorizacionServicio autorizacionServicio,
            EquipoServicio equipoServicio,
            MovimientoServicio movimientoServicio,
            StockServicio stockServicio,
            ReporteServicio reporteServicio) {
    }
}