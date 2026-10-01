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
import com.computototal.inventario.dao.memoria.CategoriaDAOMemoria;
import com.computototal.inventario.dao.memoria.ConfiguracionStockMinimoDAOMemoria;
import com.computototal.inventario.dao.memoria.EquipoDAOMemoria;
import com.computototal.inventario.dao.memoria.MovimientoDAOMemoria;
import com.computototal.inventario.dao.memoria.ProveedorDAOMemoria;
import com.computototal.inventario.dao.memoria.UbicacionDAOMemoria;
import com.computototal.inventario.dao.memoria.UsuarioDAOMemoria;
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

    public static void main(String[] args) {
        boolean demo = args.length == 1 && "--demo".equals(args[0]);
        if (args.length > 0 && !demo) {
            System.err.println("Uso: java -cp target/classes com.computototal.inventario.Main [--demo]");
            System.exit(2);
        }

        Clock reloj = demo
                ? Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneId.of("UTC"))
                : Clock.systemDefaultZone();
        Componentes componentes = crearComponentes(reloj);
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

    private static Componentes crearComponentes(Clock reloj) {
        EquipoDAO equipoDAO = new EquipoDAOMemoria();
        MovimientoDAO movimientoDAO = new MovimientoDAOMemoria();
        CategoriaDAO categoriaDAO = new CategoriaDAOMemoria();
        ConfiguracionStockMinimoDAO configuracionDAO = new ConfiguracionStockMinimoDAOMemoria();
        ProveedorDAO proveedorDAO = new ProveedorDAOMemoria();
        UbicacionDAO ubicacionDAO = new UbicacionDAOMemoria();
        UsuarioDAO usuarioDAO = new UsuarioDAOMemoria();

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