package com.computototal.inventario.consola;

import java.io.BufferedReader;
import java.io.Console;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.Movimiento;
import com.computototal.inventario.servicio.AutenticacionServicio;
import com.computototal.inventario.servicio.AutorizacionServicio;
import com.computototal.inventario.servicio.EquipoConsulta;
import com.computototal.inventario.servicio.EquipoServicio;
import com.computototal.inventario.servicio.EstadoStock;
import com.computototal.inventario.servicio.MovimientoServicio;
import com.computototal.inventario.servicio.Permiso;
import com.computototal.inventario.servicio.ReferenciaCatalogo;
import com.computototal.inventario.servicio.ReporteMovimientos;
import com.computototal.inventario.servicio.ReporteServicio;
import com.computototal.inventario.servicio.ResultadoAltaEquipo;
import com.computototal.inventario.servicio.ResultadoMovimiento;
import com.computototal.inventario.servicio.SesionUsuario;
import com.computototal.inventario.servicio.StockServicio;
import com.computototal.inventario.validacion.ValidacionesEntrada;

public final class ConsolaInventario {
    private final BufferedReader entrada;
    private final AutenticacionServicio autenticacion;
    private final AutorizacionServicio autorizacion;
    private final EquipoServicio equipos;
    private final MovimientoServicio movimientos;
    private final StockServicio stock;
    private final ReporteServicio reportes;

    public ConsolaInventario(AutenticacionServicio autenticacion, AutorizacionServicio autorizacion,
                             EquipoServicio equipos, MovimientoServicio movimientos,
                             StockServicio stock, ReporteServicio reportes) {
        this.entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        this.autenticacion = autenticacion;
        this.autorizacion = autorizacion;
        this.equipos = equipos;
        this.movimientos = movimientos;
        this.stock = stock;
        this.reportes = reportes;
    }

    public void iniciar() {
        System.out.println("Inventario de equipos informaticos");
        try {
            while (true) {
                if (autenticacion.obtenerUsuarioAutenticado().isEmpty()) {
                    if (!iniciarSesion()) {
                        break;
                    }
                } else if (!mostrarMenu()) {
                    break;
                }
            }
        } catch (EOFException finEntrada) {
            System.out.println("Fin de entrada. Sesion cerrada.");
        } catch (IOException errorLectura) {
            System.out.println("No se pudo leer la entrada: " + errorLectura.getMessage());
        } finally {
            autenticacion.cerrarSesion();
        }
    }

    private boolean iniciarSesion() throws IOException {
        System.out.println("\n1) Iniciar sesion\n0) Salir");
        String opcion = leer("Opcion: ");
        if (opcion.equals("0")) {
            return false;
        }
        if (!opcion.equals("1")) {
            System.out.println("Opcion no valida.");
            return true;
        }

        String nombre = leer("Usuario: ");
        char[] contrasena = leerContrasena();
        try {
            SesionUsuario usuario = autenticacion.iniciarSesion(nombre, contrasena);
            System.out.println("Sesion iniciada: " + usuario.nombreUsuario() + " (" + usuario.rol() + ")");
        } catch (SecurityException errorAcceso) {
            System.out.println(errorAcceso.getMessage());
        }
        return true;
    }

    private boolean mostrarMenu() throws IOException {
        SesionUsuario usuario = autenticacion.exigirSesionActiva();
        List<Opcion> opciones = crearOpciones();
        System.out.println("\nInventario | " + usuario.nombreUsuario() + " | " + usuario.rol());
        for (int indice = 0; indice < opciones.size(); indice++) {
            System.out.println((indice + 1) + ") " + opciones.get(indice).nombre());
        }
        System.out.println("0) Cerrar sesion");
        String textoOpcion = leer("Opcion: ");
        if (textoOpcion.equals("0")) {
            autenticacion.cerrarSesion();
            System.out.println("Sesion cerrada.");
            return true;
        }

        int indice;
        try {
            indice = ValidacionesEntrada.entero(textoOpcion, "opcion", 1, opciones.size()) - 1;
        } catch (IllegalArgumentException errorNumero) {
            System.out.println("Ingrese un numero de opcion valido.");
            return true;
        }
        try {
            opciones.get(indice).accion().ejecutar();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException errorEsperado) {
            System.out.println("Operacion rechazada: " + errorEsperado.getMessage());
        } catch (java.util.NoSuchElementException errorNoEncontrado) {
            System.out.println("No se encontro el registro: " + errorNoEncontrado.getMessage());
        }
        return true;
    }

    private List<Opcion> crearOpciones() {
        List<Opcion> opciones = new ArrayList<>();
        agregar(opciones, Permiso.REGISTRAR_EQUIPOS, "Registrar equipo con ingreso", this::registrarEquipo);
        agregar(opciones, Permiso.CONSULTAR_INVENTARIO, "Consultar equipo por codigo o serie", this::consultarEquipo);
        agregar(opciones, Permiso.CONSULTAR_INVENTARIO, "Listar equipos", this::listarEquipos);
        agregar(opciones, Permiso.REGISTRAR_INGRESOS, "Registrar ingreso o reingreso", this::registrarIngreso);
        agregar(opciones, Permiso.REGISTRAR_SALIDAS, "Registrar salida", this::registrarSalida);
        agregar(opciones, Permiso.REGISTRAR_TRASLADOS, "Trasladar entre sedes", this::trasladar);
        agregar(opciones, Permiso.CAMBIAR_UBICACION_EQUIPO, "Cambiar ubicacion dentro de sede", this::cambiarUbicacion);
        agregar(opciones, Permiso.CAMBIAR_ESTADO_EQUIPO, "Cambiar estado tecnico", this::cambiarEstado);
        agregar(opciones, Permiso.CONFIGURAR_STOCK_MINIMO, "Configurar stock minimo", this::configurarMinimo);
        agregar(opciones, Permiso.CONSULTAR_INVENTARIO, "Consultar existencias actuales", this::listarExistencias);
        agregar(opciones, Permiso.CONSULTAR_ALERTAS, "Consultar alertas actuales", this::listarAlertas);
        agregar(opciones, Permiso.CONSULTAR_HISTORIAL, "Consultar historial", this::consultarHistorial);
        agregar(opciones, Permiso.CONSULTAR_REPORTES, "Generar reporte por fechas", this::generarReporte);
        agregar(opciones, Permiso.ELIMINAR_REGISTROS, "Eliminar equipo permitido", this::eliminarEquipo);
        return opciones;
    }

    private void agregar(List<Opcion> opciones, Permiso permiso, String nombre, Accion accion) {
        if (autorizacion.tienePermiso(permiso)) {
            opciones.add(new Opcion(nombre, accion));
        }
    }

    private void registrarEquipo() throws IOException {
        String codigo = leer("Codigo: ");
        String serie = leer("Numero de serie: ");
        String marca = leer("Marca: ");
        String modelo = leer("Modelo: ");
        UUID categoriaId = elegirReferencia("Categoria", equipos.listarCategorias());
        UUID proveedorId = elegirReferencia("Proveedor", equipos.listarProveedores());
        UUID almacenId = elegirUbicacion(true);
        String motivo = leer("Motivo del ingreso inicial: ");
        ResultadoAltaEquipo resultado = equipos.registrarEquipoConIngreso(UUID.randomUUID(), codigo, serie,
                marca, modelo, categoriaId, proveedorId, almacenId, motivo);
        mostrarEquipo(resultado.equipo());
        resultado.ingresoInicial().ifPresent(this::mostrarResultadoMovimiento);
    }

    private void consultarEquipo() throws IOException {
        String tipo = leer("Buscar por (1) codigo o (2) serie: ");
        EquipoConsulta equipo = switch (tipo) {
            case "1" -> equipos.consultarPorCodigo(leer("Codigo: "));
            case "2" -> equipos.consultarPorNumeroSerie(leer("Numero de serie: "));
            default -> throw new IllegalArgumentException("Seleccione 1 o 2");
        };
        mostrarEquipo(equipo);
    }

    private void listarEquipos() {
        List<EquipoConsulta> lista = equipos.listar();
        if (lista.isEmpty()) {
            System.out.println("No hay equipos registrados.");
            return;
        }
        lista.forEach(this::mostrarEquipo);
    }

    private void registrarIngreso() throws IOException {
        EquipoConsulta equipo = buscarEquipoPorCodigoPrompt();
        UUID ubicacionId = elegirUbicacion(true);
        String motivo = leer("Motivo: ");
        mostrarResultadoMovimiento(movimientos.registrarIngreso(equipo.id(), ubicacionId, motivo));
    }

    private void registrarSalida() throws IOException {
        EquipoConsulta equipo = buscarEquipoPorCodigoPrompt();
        String destino = leer("Destino: ");
        String motivo = leer("Motivo: ");
        mostrarResultadoMovimiento(movimientos.registrarSalida(equipo.id(), destino, motivo));
    }

    private void trasladar() throws IOException {
        EquipoConsulta equipo = buscarEquipoPorCodigoPrompt();
        UUID ubicacionId = elegirUbicacion(true);
        String motivo = leer("Motivo: ");
        mostrarResultadoMovimiento(movimientos.trasladar(equipo.id(), ubicacionId, motivo));
    }

    private void cambiarUbicacion() throws IOException {
        EquipoConsulta equipo = buscarEquipoPorCodigoPrompt();
        UUID ubicacionId = elegirUbicacion(null);
        String motivo = leer("Motivo: ");
        mostrarResultadoMovimiento(movimientos.cambiarUbicacion(equipo.id(), ubicacionId, motivo));
    }

    private void cambiarEstado() throws IOException {
        EquipoConsulta equipo = buscarEquipoPorCodigoPrompt();
        EstadoEquipo[] estados = EstadoEquipo.values();
        for (int indice = 0; indice < estados.length; indice++) {
            System.out.println((indice + 1) + ") " + estados[indice]);
        }
        EstadoEquipo estado = estados[leerIndice("Nuevo estado: ", estados.length)];
        String justificacion = leer("Justificacion: ");
        mostrarResultadoMovimiento(movimientos.cambiarEstado(equipo.id(), estado, justificacion));
    }

    private void configurarMinimo() throws IOException {
        UUID categoriaId = elegirReferencia("Categoria", equipos.listarCategorias());
        String sede = elegirSede();
        int minimo = leerEntero("Minimo (entero >= 0): ");
        System.out.println("Minimo guardado: " + stock.configurarMinimo(categoriaId, sede, minimo).getMinimo());
        mostrarAlertas(stock.consultarAlertasActuales());
    }

    private void listarExistencias() {
        List<EstadoStock> estados = stock.listarExistencias();
        if (estados.isEmpty()) {
            System.out.println("No hay equipos ni minimos configurados para mostrar.");
            return;
        }
        for (EstadoStock estado : estados) {
            mostrarEstadoStock(estado);
        }
    }

    private void listarAlertas() {
        mostrarAlertas(stock.consultarAlertasActuales());
    }

    private void consultarHistorial() throws IOException {
        String tipo = leer("Buscar historial por (1) codigo o (2) serie: ");
        List<Movimiento> historial = switch (tipo) {
            case "1" -> movimientos.historialPorCodigo(leer("Codigo: "));
            case "2" -> movimientos.historialPorNumeroSerie(leer("Numero de serie: "));
            default -> throw new IllegalArgumentException("Seleccione 1 o 2");
        };
        if (historial.isEmpty()) {
            System.out.println("El equipo no tiene movimientos.");
            return;
        }
        historial.forEach(this::mostrarMovimiento);
    }

    private void generarReporte() throws IOException {
        String alcance = leer("Reporte (1) global o (2) por sede: ");
        LocalDate inicio = leerFecha("Fecha inicial (AAAA-MM-DD): ");
        LocalDate fin = leerFecha("Fecha final (AAAA-MM-DD): ");
        ReporteMovimientos reporte = switch (alcance) {
            case "1" -> reportes.generarGlobal(inicio, fin);
            case "2" -> reportes.generarPorSede(inicio, fin, elegirSede());
            default -> throw new IllegalArgumentException("Seleccione 1 o 2");
        };
        mostrarReporte(reporte);
    }

    private void eliminarEquipo() throws IOException {
        EquipoConsulta equipo = buscarEquipoPorCodigoPrompt();
        equipos.eliminar(equipo.id());
        System.out.println("Equipo eliminado.");
    }

    private EquipoConsulta buscarEquipoPorCodigoPrompt() throws IOException {
        return equipos.consultarPorCodigo(leer("Codigo del equipo: "));
    }

    private UUID elegirReferencia(String titulo, List<ReferenciaCatalogo> opciones) throws IOException {
        if (opciones.isEmpty()) {
            throw new IllegalStateException("No hay " + titulo.toLowerCase() + "s disponibles");
        }
        System.out.println(titulo + ":");
        for (int indice = 0; indice < opciones.size(); indice++) {
            System.out.println((indice + 1) + ") " + opciones.get(indice).nombre());
        }
        return opciones.get(leerIndice("Seleccione: ", opciones.size())).id();
    }

    private UUID elegirUbicacion(Boolean soloAlmacen) throws IOException {
        List<EquipoConsulta.UbicacionResumen> opciones = equipos.listarUbicaciones().stream()
                .filter(ubicacion -> soloAlmacen == null || ubicacion.almacen() == soloAlmacen)
                .toList();
        if (opciones.isEmpty()) {
            throw new IllegalStateException("No hay ubicaciones disponibles para esa operacion");
        }
        System.out.println("Ubicaciones:");
        for (int indice = 0; indice < opciones.size(); indice++) {
            EquipoConsulta.UbicacionResumen ubicacion = opciones.get(indice);
            System.out.println((indice + 1) + ") " + ubicacion.sede() + " | " + ubicacion.ambiente()
                    + " | " + ubicacion.area() + " | piso " + ubicacion.piso()
                    + (ubicacion.almacen() ? " | almacen" : ""));
        }
        return opciones.get(leerIndice("Seleccione: ", opciones.size())).id();
    }

    private String elegirSede() throws IOException {
        Map<String, String> sedes = new LinkedHashMap<>();
        equipos.listarUbicaciones().forEach(ubicacion ->
                sedes.putIfAbsent(ubicacion.sede().toLowerCase(java.util.Locale.ROOT), ubicacion.sede()));
        List<String> opciones = List.copyOf(sedes.values());
        for (int indice = 0; indice < opciones.size(); indice++) {
            System.out.println((indice + 1) + ") " + opciones.get(indice));
        }
        return opciones.get(leerIndice("Sede: ", opciones.size()));
    }

    private int leerIndice(String mensaje, int total) throws IOException {
        return ValidacionesEntrada.entero(leer(mensaje), "opcion", 1, total) - 1;
    }

    private int leerEntero(String mensaje) throws IOException {
        return ValidacionesEntrada.entero(leer(mensaje), "numero", Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private LocalDate leerFecha(String mensaje) throws IOException {
        return ValidacionesEntrada.fecha(leer(mensaje), "fecha");
    }

    private char[] leerContrasena() throws IOException {
        Console consola = System.console();
        if (consola != null) {
            char[] contrasena = consola.readPassword("Contrasena: ");
            if (contrasena == null) {
                throw new EOFException();
            }
            return contrasena;
        }
        return leer("Contrasena: ").toCharArray();
    }

    private String leer(String mensaje) throws IOException {
        System.out.print(mensaje);
        String linea = entrada.readLine();
        if (linea == null) {
            throw new EOFException();
        }
        return linea.strip();
    }

    private void mostrarEquipo(EquipoConsulta equipo) {
        System.out.println("Equipo " + equipo.codigo() + " | serie " + equipo.numeroSerie()
                + " | " + equipo.marca() + " " + equipo.modelo() + " | " + equipo.categoria()
                + " | estado " + equipo.estado() + " | presente " + equipo.presenteEnAlmacen());
        equipo.ubicacionActual().ifPresent(ubicacion -> System.out.println("  Ubicacion: "
                + ubicacion.sede() + " / " + ubicacion.ambiente() + " / " + ubicacion.area() + " / " + ubicacion.piso()));
        equipo.ultimaUbicacionAlmacen().ifPresent(ubicacion -> System.out.println("  Ultimo almacen: "
                + ubicacion.sede() + " / " + ubicacion.ambiente()));
        equipo.destinoSalida().ifPresent(destino -> System.out.println("  Destino de salida: " + destino));
    }

    private void mostrarResultadoMovimiento(ResultadoMovimiento resultado) {
        Movimiento movimiento = resultado.movimiento();
        System.out.println("Movimiento registrado: " + movimiento.getTipo() + " | "
                + movimiento.getEquipo().codigo() + " | " + movimiento.getFechaHora());
        mostrarAlertas(resultado.estadosStockAfectados().stream().filter(EstadoStock::tieneAlerta).toList());
        if (resultado.estadosStockAfectados().stream().noneMatch(EstadoStock::tieneAlerta)) {
            System.out.println("Sin alertas para las sedes afectadas.");
        }
    }

    private void mostrarMovimiento(Movimiento movimiento) {
        System.out.println(movimiento.getFechaHora() + " | " + movimiento.getTipo() + " | "
                + movimiento.getEquipo().codigo() + " | " + movimiento.getNombreResponsable()
                + " | " + movimiento.getMotivo());
        movimiento.getUbicacionOrigen().ifPresent(origen -> System.out.println("  Origen: "
                + origen.sede() + " / " + origen.ambiente()));
        movimiento.getUbicacionDestino().ifPresent(destino -> System.out.println("  Destino: "
                + destino.sede() + " / " + destino.ambiente()));
        movimiento.getDestinoExterno().ifPresent(destino -> System.out.println("  Destino externo: " + destino));
        if (movimiento.getEstadoAnterior().isPresent()) {
            System.out.println("  Estado: " + movimiento.getEstadoAnterior().orElseThrow()
                    + " -> " + movimiento.getEstadoNuevo().orElseThrow());
        }
    }

    private void mostrarEstadoStock(EstadoStock estado) {
        System.out.println(estado.categoria() + " | " + estado.sede() + " | presentes " + estado.totalPresente()
                + " | disponibles " + estado.disponible() + " | minimo "
                + estado.minimoConfigurado().map(String::valueOf).orElse("sin configurar")
                + (estado.tieneAlerta() ? " | ALERTA" : ""));
    }

    private void mostrarAlertas(List<EstadoStock> alertas) {
        if (alertas.isEmpty()) {
            System.out.println("No hay alertas activas.");
            return;
        }
        alertas.forEach(this::mostrarEstadoStock);
    }

    private void mostrarReporte(ReporteMovimientos reporte) {
        System.out.println("Reporte " + reporte.inicio() + " a " + reporte.fin()
                + reporte.sede().map(sede -> " | " + sede).orElse(" | Global"));
        System.out.println("Movimientos del periodo: " + reporte.movimientos().size());
        reporte.movimientos().forEach(this::mostrarMovimiento);
        System.out.println("Entradas " + reporte.entradas() + " | salidas " + reporte.salidas()
                + " | traslados recibidos " + reporte.trasladosRecibidos()
                + " | enviados " + reporte.trasladosEnviados());
        System.out.println("Saldo historico inicial " + reporte.saldoInicial()
                + " | saldo historico final " + reporte.saldoFinal());
        reporte.saldosPorCategoria().forEach(saldo -> System.out.println("  " + saldo.categoria()
                + ": inicial " + saldo.saldoInicial() + ", final " + saldo.saldoFinal()));
        System.out.println("Existencias actuales (dato separado):");
        reporte.existenciasActuales().forEach(this::mostrarEstadoStock);
    }

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws IOException;
    }

    private record Opcion(String nombre, Accion accion) {
    }
}