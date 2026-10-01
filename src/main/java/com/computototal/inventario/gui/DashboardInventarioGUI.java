package com.computototal.inventario.gui;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

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

public final class DashboardInventarioGUI {
    private static final Color VERDE = new Color(27, 103, 81);
    private static final Color VERDE_OSCURO = new Color(26, 56, 50);
    private static final Color FONDO = new Color(244, 247, 245);
    private static final Color TEXTO = new Color(37, 49, 45);
    private static final Color SECUNDARIO = new Color(104, 117, 111);
    private static final Color BORDE = new Color(220, 228, 223);

    private final AutenticacionServicio autenticacion;
    private final AutorizacionServicio autorizacion;
    private final EquipoServicio equipos;
    private final MovimientoServicio movimientos;
    private final StockServicio stock;
    private final ReporteServicio reportes;
    private final List<Modulo> modulos;
    private JFrame ventana;
    private JPanel panelResultado;
    private JLabel estadoOperacion;

    public DashboardInventarioGUI(AutenticacionServicio autenticacion, AutorizacionServicio autorizacion,
                                  EquipoServicio equipos, MovimientoServicio movimientos,
                                  StockServicio stock, ReporteServicio reportes) {
        this.autenticacion = Objects.requireNonNull(autenticacion, "autenticacion");
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
        this.equipos = Objects.requireNonNull(equipos, "equipos");
        this.movimientos = Objects.requireNonNull(movimientos, "movimientos");
        this.stock = Objects.requireNonNull(stock, "stock");
        this.reportes = Objects.requireNonNull(reportes, "reportes");
        this.modulos = Arrays.stream(Modulo.values())
                .filter(modulo -> autorizacion.tienePermiso(modulo.permiso))
                .toList();
    }

    public void mostrar() {
        SwingUtilities.invokeLater(() -> abrir(Modulo.INICIO));
    }

    private void abrir(Modulo modulo) {
        JFrame anterior = ventana;
        ventana = null;
        if (anterior != null) {
            anterior.dispose();
        }

        JFrame nuevaVentana = new JFrame("Inventario | COMPUTO TOTAL");
        nuevaVentana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        nuevaVentana.setMinimumSize(new Dimension(980, 680));
        nuevaVentana.setSize(1180, 780);
        nuevaVentana.setLocationRelativeTo(null);
        nuevaVentana.setLayout(new BorderLayout());
        JScrollPane navegacion = new JScrollPane(crearNavegacion(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        navegacion.setPreferredSize(new Dimension(245, 0));
        navegacion.setBorder(null);
        navegacion.getViewport().setBackground(VERDE_OSCURO);
        nuevaVentana.add(navegacion, BorderLayout.WEST);
        nuevaVentana.add(modulo == Modulo.INICIO ? crearInicio() : crearPaginaModulo(modulo), BorderLayout.CENTER);
        nuevaVentana.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evento) {
                cerrarSesion();
            }
        });
        ventana = nuevaVentana;
        nuevaVentana.setVisible(true);
    }

    private JPanel crearNavegacion() {
        JPanel barra = new JPanel();
        barra.setBackground(VERDE_OSCURO);
        barra.setBorder(BorderFactory.createEmptyBorder(24, 15, 17, 15));
        barra.setPreferredSize(new Dimension(245, 0));
        barra.setLayout(new BoxLayout(barra, BoxLayout.Y_AXIS));

        JLabel marca = new JLabel("CT  /  INVENTARIO");
        marca.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        marca.setForeground(Color.WHITE);
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(marca);
        barra.add(Box.createVerticalStrut(28));

        SesionUsuario usuario = autenticacion.exigirSesionActiva();
        JLabel identidad = new JLabel("<html><b>" + escaparHtml(usuario.nombreUsuario()) + "</b><br>"
                + escaparHtml(usuario.rol().toString()) + "</html>");
        identidad.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        identidad.setForeground(new Color(218, 231, 224));
        identidad.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(identidad);
        barra.add(Box.createVerticalStrut(26));

        String seccionActual = "";
        for (Modulo modulo : modulos) {
            if (modulo == Modulo.INICIO) {
                continue;
            }
            if (!modulo.seccion.equals(seccionActual)) {
                seccionActual = modulo.seccion;
                JLabel tituloSeccion = new JLabel(seccionActual.toUpperCase());
                tituloSeccion.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                tituloSeccion.setForeground(new Color(158, 186, 173));
                tituloSeccion.setAlignmentX(Component.LEFT_ALIGNMENT);
                barra.add(tituloSeccion);
                barra.add(Box.createVerticalStrut(5));
            }
            JButton boton = crearBotonNavegacion(modulo.nombre, modulo.icono);
            boton.addActionListener(evento -> abrir(modulo));
            barra.add(boton);
        }

        barra.add(Box.createVerticalGlue());
        JButton inicio = crearBotonNavegacion("Panel principal", "inicio");
        inicio.addActionListener(evento -> abrir(Modulo.INICIO));
        barra.add(inicio);
        JButton cerrarSesion = crearBotonNavegacion("Cerrar sesion", "salir");
        cerrarSesion.setForeground(new Color(255, 220, 209));
        cerrarSesion.addActionListener(evento -> cerrarSesion());
        barra.add(cerrarSesion);
        return barra;
    }

    private JButton crearBotonNavegacion(String texto, String tipoIcono) {
        JButton boton = new JButton(texto);
        boton.setIcon(new IconoNavegacion(tipoIcono));
        boton.setIconTextGap(11);
        boton.setToolTipText(texto);
        boton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        boton.setForeground(new Color(236, 243, 239));
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        boton.setPreferredSize(new Dimension(210, 34));
        boton.setContentAreaFilled(false);
        boton.setBorder(BorderFactory.createEmptyBorder(6, 9, 6, 4));
        boton.setFocusPainted(false);
        return boton;
    }

    private JPanel crearInicio() {
        JPanel contenido = new JPanel(new BorderLayout(0, 22));
        contenido.setBackground(FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(30, 34, 30, 34));

        SesionUsuario usuario = autenticacion.exigirSesionActiva();
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        JLabel titulo = new JLabel("Panel de control");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        titulo.setForeground(TEXTO);
        JLabel fecha = new JLabel(LocalDate.now().toString() + "  |  " + usuario.nombreUsuario());
        fecha.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        fecha.setForeground(SECUNDARIO);
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(fecha, BorderLayout.SOUTH);
        contenido.add(encabezado, BorderLayout.NORTH);

        List<EquipoConsulta> inventario = equipos.listar();
        List<EstadoStock> alertas = autorizacion.tienePermiso(Permiso.CONSULTAR_ALERTAS)
                ? stock.consultarAlertasActuales() : List.of();
        JPanel resumen = new JPanel(new java.awt.GridLayout(1, 3, 14, 0));
        resumen.setOpaque(false);
        resumen.add(crearIndicador("Equipos registrados", String.valueOf(inventario.size()), "Inventario"));
        resumen.add(crearIndicador("Alertas de stock", String.valueOf(alertas.size()), "Stock minimo"));
        resumen.add(crearIndicador("Acciones disponibles", String.valueOf(modulos.size() - 1), usuario.rol().toString()));

        JPanel zona = new JPanel(new BorderLayout(0, 13));
        zona.setOpaque(false);
        JLabel tituloTabla = new JLabel("Inventario actual");
        tituloTabla.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        tituloTabla.setForeground(TEXTO);
        zona.add(tituloTabla, BorderLayout.NORTH);
        zona.add(crearTablaEquipos(inventario), BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 22));
        centro.setOpaque(false);
        centro.add(resumen, BorderLayout.NORTH);
        centro.add(zona, BorderLayout.CENTER);
        contenido.add(centro, BorderLayout.CENTER);
        return contenido;
    }

    private JPanel crearIndicador(String etiqueta, String valor, String detalle) {
        JPanel indicador = new JPanel();
        indicador.setBackground(Color.WHITE);
        indicador.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE), BorderFactory.createEmptyBorder(15, 17, 15, 17)));
        indicador.setLayout(new BoxLayout(indicador, BoxLayout.Y_AXIS));
        JLabel nombre = new JLabel(etiqueta);
        nombre.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        nombre.setForeground(SECUNDARIO);
        JLabel cantidad = new JLabel(valor);
        cantidad.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        cantidad.setForeground(VERDE_OSCURO);
        JLabel pie = new JLabel(detalle);
        pie.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        pie.setForeground(SECUNDARIO);
        indicador.add(nombre);
        indicador.add(Box.createVerticalStrut(8));
        indicador.add(cantidad);
        indicador.add(Box.createVerticalStrut(5));
        indicador.add(pie);
        return indicador;
    }

    private JPanel crearPaginaModulo(Modulo modulo) {
        JPanel pagina = new JPanel(new BorderLayout(0, 20));
        pagina.setBackground(FONDO);
        pagina.setBorder(BorderFactory.createEmptyBorder(30, 34, 28, 34));

        JLabel titulo = new JLabel(modulo.nombre);
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        titulo.setForeground(TEXTO);
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(titulo, BorderLayout.NORTH);
        estadoOperacion = new JLabel(" ");
        estadoOperacion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        estadoOperacion.setForeground(SECUNDARIO);
        encabezado.add(estadoOperacion, BorderLayout.SOUTH);
        pagina.add(encabezado, BorderLayout.NORTH);

        panelResultado = new JPanel(new BorderLayout());
        panelResultado.setBackground(Color.WHITE);
        panelResultado.setBorder(BorderFactory.createLineBorder(BORDE));
        JPanel formulario = crearFormulario();
        JPanel cuerpo = new JPanel();
        cuerpo.setBackground(FONDO);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.add(formulario);
        cuerpo.add(Box.createVerticalStrut(16));
        cuerpo.add(panelResultado);
        JScrollPane desplazamiento = new JScrollPane(cuerpo);
        desplazamiento.setBorder(null);
        desplazamiento.getViewport().setBackground(FONDO);
        pagina.add(desplazamiento, BorderLayout.CENTER);

        try {
            configurarModulo(modulo, formulario);
        } catch (RuntimeException error) {
            mostrarError(error);
        }
        return pagina;
    }

    private JPanel crearFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE), BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        return formulario;
    }

    private void configurarModulo(Modulo modulo, JPanel formulario) {
        switch (modulo) {
            case REGISTRAR_EQUIPO -> formularioAltaEquipo(formulario);
            case CONSULTAR_EQUIPO -> formularioConsulta(formulario, false);
            case LISTAR_EQUIPOS -> ejecutarYMostrar(formulario, "Actualizar lista", () ->
                    mostrarEquipos(equipos.listar()));
            case REGISTRAR_INGRESO -> formularioIngreso(formulario);
            case REGISTRAR_SALIDA -> formularioSalida(formulario);
            case TRASLADAR -> formularioTraslado(formulario);
            case CAMBIAR_UBICACION -> formularioCambioUbicacion(formulario);
            case CAMBIAR_ESTADO -> formularioCambioEstado(formulario);
            case CONFIGURAR_MINIMO -> formularioMinimo(formulario);
            case EXISTENCIAS -> ejecutarYMostrar(formulario, "Actualizar existencias", () ->
                    mostrarStock(stock.listarExistencias()));
            case ALERTAS -> ejecutarYMostrar(formulario, "Actualizar alertas", () ->
                    mostrarStock(stock.consultarAlertasActuales()));
            case HISTORIAL -> formularioHistorial(formulario);
            case REPORTE -> formularioReporte(formulario);
            case ELIMINAR_EQUIPO -> formularioEliminar(formulario);
            case INICIO -> throw new IllegalArgumentException("El panel principal no es un modulo de operacion");
        }
    }

    private void formularioAltaEquipo(JPanel formulario) {
        JTextField codigo = campoTexto();
        JTextField serie = campoTexto();
        JTextField marca = campoTexto();
        JTextField modelo = campoTexto();
        JComboBox<Elemento> categoria = comboReferencias(equipos.listarCategorias());
        JComboBox<Elemento> proveedor = comboReferencias(equipos.listarProveedores());
        JComboBox<Elemento> ubicacion = comboUbicaciones(true);
        JTextField motivo = campoTexto();
        int fila = 0;
        fila = agregarCampo(formulario, fila, "Codigo", codigo);
        fila = agregarCampo(formulario, fila, "Numero de serie", serie);
        fila = agregarCampo(formulario, fila, "Marca", marca);
        fila = agregarCampo(formulario, fila, "Modelo", modelo);
        fila = agregarCampo(formulario, fila, "Categoria", categoria);
        fila = agregarCampo(formulario, fila, "Proveedor", proveedor);
        fila = agregarCampo(formulario, fila, "Almacen inicial", ubicacion);
        fila = agregarCampo(formulario, fila, "Motivo del ingreso", motivo);
        agregarAccion(formulario, fila, "Registrar equipo", () -> {
            ResultadoAltaEquipo alta = equipos.registrarEquipoConIngreso(UUID.randomUUID(), texto(codigo),
                    texto(serie), texto(marca), texto(modelo), idSeleccionado(categoria),
                    idSeleccionado(proveedor), idSeleccionado(ubicacion), texto(motivo));
            mostrarMensaje("Equipo " + alta.equipo().codigo() + " registrado.");
        });
    }

    private void formularioConsulta(JPanel formulario, boolean historial) {
        JComboBox<String> criterio = new JComboBox<>(new String[]{"Codigo", "Numero de serie"});
        JTextField valor = campoTexto();
        int fila = agregarCampo(formulario, 0, historial ? "Buscar historial por" : "Buscar equipo por", criterio);
        fila = agregarCampo(formulario, fila, "Valor", valor);
        agregarAccion(formulario, fila, historial ? "Consultar historial" : "Consultar equipo", () -> {
            String dato = texto(valor);
            if (historial) {
                List<Movimiento> lista = criterio.getSelectedIndex() == 0
                        ? movimientos.historialPorCodigo(dato) : movimientos.historialPorNumeroSerie(dato);
                mostrarMovimientos(lista);
            } else {
                EquipoConsulta equipo = criterio.getSelectedIndex() == 0
                        ? equipos.consultarPorCodigo(dato) : equipos.consultarPorNumeroSerie(dato);
                mostrarEquipos(List.of(equipo));
            }
        });
    }

    private void formularioIngreso(JPanel formulario) {
        JTextField codigo = campoTexto();
        JComboBox<Elemento> ubicacion = comboUbicaciones(true);
        JTextField motivo = campoTexto();
        int fila = agregarCampo(formulario, 0, "Codigo del equipo", codigo);
        fila = agregarCampo(formulario, fila, "Almacen de destino", ubicacion);
        fila = agregarCampo(formulario, fila, "Motivo", motivo);
        agregarAccion(formulario, fila, "Registrar ingreso", () -> {
            EquipoConsulta equipo = equipos.consultarPorCodigo(texto(codigo));
            mostrarResultadoMovimiento(movimientos.registrarIngreso(equipo.id(), idSeleccionado(ubicacion), texto(motivo)));
        });
    }

    private void formularioSalida(JPanel formulario) {
        JTextField codigo = campoTexto();
        JTextField destino = campoTexto();
        JTextField motivo = campoTexto();
        int fila = agregarCampo(formulario, 0, "Codigo del equipo", codigo);
        fila = agregarCampo(formulario, fila, "Destino externo", destino);
        fila = agregarCampo(formulario, fila, "Motivo", motivo);
        agregarAccion(formulario, fila, "Registrar salida", () -> {
            EquipoConsulta equipo = equipos.consultarPorCodigo(texto(codigo));
            mostrarResultadoMovimiento(movimientos.registrarSalida(equipo.id(), texto(destino), texto(motivo)));
        });
    }

    private void formularioTraslado(JPanel formulario) {
        JTextField codigo = campoTexto();
        JComboBox<Elemento> ubicacion = comboUbicaciones(true);
        JTextField motivo = campoTexto();
        int fila = agregarCampo(formulario, 0, "Codigo del equipo", codigo);
        fila = agregarCampo(formulario, fila, "Almacen de destino", ubicacion);
        fila = agregarCampo(formulario, fila, "Motivo", motivo);
        agregarAccion(formulario, fila, "Registrar traslado", () -> {
            EquipoConsulta equipo = equipos.consultarPorCodigo(texto(codigo));
            mostrarResultadoMovimiento(movimientos.trasladar(equipo.id(), idSeleccionado(ubicacion), texto(motivo)));
        });
    }

    private void formularioCambioUbicacion(JPanel formulario) {
        JTextField codigo = campoTexto();
        JComboBox<Elemento> ubicacion = comboUbicaciones(null);
        JTextField motivo = campoTexto();
        int fila = agregarCampo(formulario, 0, "Codigo del equipo", codigo);
        fila = agregarCampo(formulario, fila, "Nueva ubicacion", ubicacion);
        fila = agregarCampo(formulario, fila, "Motivo", motivo);
        agregarAccion(formulario, fila, "Cambiar ubicacion", () -> {
            EquipoConsulta equipo = equipos.consultarPorCodigo(texto(codigo));
            mostrarResultadoMovimiento(movimientos.cambiarUbicacion(equipo.id(), idSeleccionado(ubicacion), texto(motivo)));
        });
    }

    private void formularioCambioEstado(JPanel formulario) {
        JTextField codigo = campoTexto();
        JComboBox<EstadoEquipo> estado = new JComboBox<>(EstadoEquipo.values());
        JTextField motivo = campoTexto();
        int fila = agregarCampo(formulario, 0, "Codigo del equipo", codigo);
        fila = agregarCampo(formulario, fila, "Nuevo estado", estado);
        fila = agregarCampo(formulario, fila, "Justificacion", motivo);
        agregarAccion(formulario, fila, "Actualizar estado", () -> {
            EquipoConsulta equipo = equipos.consultarPorCodigo(texto(codigo));
            mostrarResultadoMovimiento(movimientos.cambiarEstado(equipo.id(), (EstadoEquipo) estado.getSelectedItem(),
                    texto(motivo)));
        });
    }

    private void formularioMinimo(JPanel formulario) {
        JComboBox<Elemento> categoria = comboReferencias(equipos.listarCategorias());
        JComboBox<Elemento> sede = comboSedes();
        JSpinner minimo = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        int fila = agregarCampo(formulario, 0, "Categoria", categoria);
        fila = agregarCampo(formulario, fila, "Sede", sede);
        fila = agregarCampo(formulario, fila, "Stock minimo", minimo);
        agregarAccion(formulario, fila, "Guardar minimo", () -> {
            stock.configurarMinimo(idSeleccionado(categoria), textoSeleccionado(sede), (Integer) minimo.getValue());
            mostrarMensaje("Stock minimo actualizado.");
        });
    }

    private void formularioHistorial(JPanel formulario) {
        formularioConsulta(formulario, true);
    }

    private void formularioReporte(JPanel formulario) {
        JComboBox<String> alcance = new JComboBox<>(new String[]{"Global", "Por sede"});
        JTextField inicio = campoTexto();
        inicio.setText(LocalDate.now().withDayOfMonth(1).toString());
        JTextField fin = campoTexto();
        fin.setText(LocalDate.now().toString());
        JComboBox<Elemento> sede = comboSedes();
        int fila = agregarCampo(formulario, 0, "Alcance", alcance);
        fila = agregarCampo(formulario, fila, "Fecha inicial (AAAA-MM-DD)", inicio);
        fila = agregarCampo(formulario, fila, "Fecha final (AAAA-MM-DD)", fin);
        fila = agregarCampo(formulario, fila, "Sede", sede);
        alcance.addActionListener(evento -> sede.setEnabled(alcance.getSelectedIndex() == 1));
        sede.setEnabled(false);
        agregarAccion(formulario, fila, "Generar reporte", () -> {
            LocalDate fechaInicio = leerFecha(inicio);
            LocalDate fechaFin = leerFecha(fin);
            ReporteMovimientos reporte = alcance.getSelectedIndex() == 0
                    ? reportes.generarGlobal(fechaInicio, fechaFin)
                    : reportes.generarPorSede(fechaInicio, fechaFin, textoSeleccionado(sede));
            mostrarReporte(reporte);
        });
    }

    private void formularioEliminar(JPanel formulario) {
        JTextField codigo = campoTexto();
        int fila = agregarCampo(formulario, 0, "Codigo del equipo", codigo);
        agregarAccion(formulario, fila, "Eliminar equipo", () -> {
            EquipoConsulta equipo = equipos.consultarPorCodigo(texto(codigo));
            int confirmacion = JOptionPane.showConfirmDialog(ventana,
                    "Se eliminara el equipo " + equipo.codigo() + ". ¿Deseas continuar?",
                    "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmacion == JOptionPane.YES_OPTION) {
                equipos.eliminar(equipo.id());
                mostrarMensaje("Equipo eliminado.");
            }
        });
    }

    private void ejecutarYMostrar(JPanel formulario, String textoBoton, Runnable accion) {
        agregarAccion(formulario, 0, textoBoton, accion);
        accion.run();
    }

    private int agregarCampo(JPanel formulario, int fila, String etiqueta, Component control) {
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.gridx = 0;
        restricciones.gridy = fila;
        restricciones.anchor = GridBagConstraints.WEST;
        restricciones.insets = new Insets(5, 0, 5, 16);
        JLabel nombre = new JLabel(etiqueta);
        nombre.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        nombre.setForeground(TEXTO);
        formulario.add(nombre, restricciones);

        restricciones.gridx = 1;
        restricciones.weightx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        restricciones.insets = new Insets(5, 0, 5, 0);
        control.setPreferredSize(new Dimension(390, 38));
        formulario.add(control, restricciones);
        return fila + 1;
    }

    private void agregarAccion(JPanel formulario, int fila, String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.setIcon(new IconoNavegacion("aceptar"));
        boton.setIconTextGap(8);
        boton.setToolTipText(texto);
        boton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        boton.setForeground(Color.WHITE);
        boton.setBackground(VERDE);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setPreferredSize(new Dimension(180, 40));
        boton.addActionListener(evento -> ejecutar(accion));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.gridx = 1;
        restricciones.gridy = fila;
        restricciones.anchor = GridBagConstraints.WEST;
        restricciones.insets = new Insets(13, 0, 0, 0);
        formulario.add(boton, restricciones);
    }

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
            estadoOperacion.setText("Operacion completada");
            estadoOperacion.setForeground(VERDE);
        } catch (RuntimeException error) {
            mostrarError(error);
        }
    }

    private void mostrarError(RuntimeException error) {
        String mensaje = error.getMessage() == null ? "No se pudo completar la operacion." : error.getMessage();
        estadoOperacion.setText("Revisa los datos ingresados");
        estadoOperacion.setForeground(new Color(176, 55, 48));
        JOptionPane.showMessageDialog(ventana, mensaje, "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarMensaje(String texto) {
        panelResultado.removeAll();
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        etiqueta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        etiqueta.setForeground(VERDE_OSCURO);
        panelResultado.add(etiqueta, BorderLayout.NORTH);
        refrescarResultado();
    }

    private void mostrarEquipos(List<EquipoConsulta> lista) {
        List<Object[]> filas = lista.stream().map(equipo -> new Object[]{equipo.codigo(), equipo.numeroSerie(),
                equipo.marca() + " " + equipo.modelo(), equipo.categoria(), equipo.proveedor(), equipo.estado(),
                equipo.ubicacionActual().map(this::ubicacionTexto).orElse("Fuera de almacen")}).toList();
        mostrarTabla(new String[]{"Codigo", "Serie", "Equipo", "Categoria", "Proveedor", "Estado", "Ubicacion"}, filas);
    }

    private JPanel crearTablaEquipos(List<EquipoConsulta> lista) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(BORDE));
        List<Object[]> filas = lista.stream().map(equipo -> new Object[]{equipo.codigo(), equipo.numeroSerie(),
                equipo.marca() + " " + equipo.modelo(), equipo.categoria(), equipo.estado(),
                equipo.ubicacionActual().map(this::ubicacionTexto).orElse("Fuera de almacen")}).toList();
        panel.add(crearTabla(new String[]{"Codigo", "Serie", "Equipo", "Categoria", "Estado", "Ubicacion"}, filas),
                BorderLayout.CENTER);
        return panel;
    }

    private void mostrarStock(List<EstadoStock> lista) {
        List<Object[]> filas = lista.stream().map(estado -> new Object[]{estado.categoria(), estado.sede(),
                estado.totalPresente(), estado.disponible(),
                estado.minimoConfigurado().map(String::valueOf).orElse("Sin configurar"),
                estado.tieneAlerta() ? "Alerta" : "OK"}).toList();
        mostrarTabla(new String[]{"Categoria", "Sede", "En almacen", "Disponibles", "Minimo", "Estado"}, filas);
    }

    private void mostrarMovimientos(List<Movimiento> lista) {
        List<Object[]> filas = lista.stream().map(movimiento -> new Object[]{movimiento.getFechaHora(),
                movimiento.getTipo(), movimiento.getEquipo().codigo(), movimiento.getNombreResponsable(),
                movimiento.getMotivo(), movimiento.getUbicacionOrigen().map(this::ubicacionTexto).orElse("-"),
                movimiento.getUbicacionDestino().map(this::ubicacionTexto)
                        .orElseGet(() -> movimiento.getDestinoExterno().orElse("-"))}).toList();
        mostrarTabla(new String[]{"Fecha", "Tipo", "Codigo", "Responsable", "Motivo", "Origen", "Destino"}, filas);
    }

    private void mostrarReporte(ReporteMovimientos reporte) {
        panelResultado.removeAll();
        JPanel salida = new JPanel(new BorderLayout(0, 12));
        salida.setBackground(Color.WHITE);
        salida.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        String alcance = reporte.sede().map(sede -> "Sede " + sede).orElse("Global");
        JLabel resumen = new JLabel("" + alcance + "  |  " + reporte.inicio() + " a " + reporte.fin()
                + "  |  Entradas " + reporte.entradas() + "  |  Salidas " + reporte.salidas()
                + "  |  Traslados recibidos " + reporte.trasladosRecibidos()
                + "  |  enviados " + reporte.trasladosEnviados()
                + "  |  Saldo " + reporte.saldoInicial() + " -> " + reporte.saldoFinal());
        resumen.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        resumen.setForeground(TEXTO);
        salida.add(resumen, BorderLayout.NORTH);
        List<Object[]> filas = reporte.movimientos().stream().map(movimiento -> new Object[]{movimiento.getFechaHora(),
                movimiento.getTipo(), movimiento.getEquipo().codigo(), movimiento.getNombreResponsable(),
                movimiento.getMotivo()}).toList();
        salida.add(crearTabla(new String[]{"Fecha", "Tipo", "Codigo", "Responsable", "Motivo"}, filas),
                BorderLayout.CENTER);
        panelResultado.add(salida, BorderLayout.CENTER);
        refrescarResultado();
    }

    private void mostrarResultadoMovimiento(ResultadoMovimiento resultado) {
        Movimiento movimiento = resultado.movimiento();
        String resumen = "Movimiento " + movimiento.getTipo() + " registrado para "
                + movimiento.getEquipo().codigo() + " el " + movimiento.getFechaHora();
        if (resultado.estadosStockAfectados().stream().anyMatch(EstadoStock::tieneAlerta)) {
            resumen += ". Hay alertas de stock en las sedes afectadas.";
        }
        mostrarMensaje(resumen);
    }

    private void mostrarTabla(String[] columnas, List<Object[]> filas) {
        panelResultado.removeAll();
        panelResultado.add(crearTabla(columnas, filas), BorderLayout.CENTER);
        refrescarResultado();
    }

    private JScrollPane crearTabla(String[] columnas, List<Object[]> filas) {
        DefaultTableModel modelo = new DefaultTableModel(filas.toArray(Object[][]::new), columnas) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        tabla.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tabla.setRowHeight(29);
        tabla.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        tabla.setFillsViewportHeight(true);
        JScrollPane desplazamiento = new JScrollPane(tabla);
        desplazamiento.setBorder(BorderFactory.createEmptyBorder());
        desplazamiento.setPreferredSize(new Dimension(680, Math.max(90, Math.min(340, 32 + filas.size() * 29))));
        return desplazamiento;
    }

    private void refrescarResultado() {
        panelResultado.revalidate();
        panelResultado.repaint();
    }

    private JComboBox<Elemento> comboReferencias(List<ReferenciaCatalogo> opciones) {
        return comboElementos(opciones.stream().map(opcion -> new Elemento(opcion.id(), opcion.nombre())).toList());
    }

    private JComboBox<Elemento> comboUbicaciones(Boolean soloAlmacen) {
        List<Elemento> opciones = equipos.listarUbicaciones().stream()
                .filter(ubicacion -> soloAlmacen == null || ubicacion.almacen() == soloAlmacen)
                .map(ubicacion -> new Elemento(ubicacion.id(), ubicacionTexto(ubicacion)))
                .toList();
        return comboElementos(opciones);
    }

    private JComboBox<Elemento> comboSedes() {
        Map<String, Elemento> sedes = new LinkedHashMap<>();
        equipos.listarUbicaciones().forEach(ubicacion -> sedes.putIfAbsent(
                ubicacion.sede().toLowerCase(java.util.Locale.ROOT), new Elemento(ubicacion.id(), ubicacion.sede())));
        return comboElementos(List.copyOf(sedes.values()));
    }

    private JComboBox<Elemento> comboElementos(List<Elemento> opciones) {
        return new JComboBox<>(opciones.toArray(Elemento[]::new));
    }

    private JTextField campoTexto() {
        JTextField campo = new JTextField();
        campo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        campo.setForeground(TEXTO);
        campo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        return campo;
    }

    private UUID idSeleccionado(JComboBox<Elemento> combo) {
        Elemento seleccion = (Elemento) combo.getSelectedItem();
        if (seleccion == null) {
            throw new IllegalStateException("No hay opciones disponibles para seleccionar");
        }
        return seleccion.id();
    }

    private String textoSeleccionado(JComboBox<Elemento> combo) {
        Elemento seleccion = (Elemento) combo.getSelectedItem();
        if (seleccion == null) {
            throw new IllegalStateException("No hay opciones disponibles para seleccionar");
        }
        return seleccion.nombre();
    }

    private String texto(JTextField campo) {
        String valor = campo.getText().strip();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("Completa todos los campos requeridos");
        }
        return valor;
    }

    private LocalDate leerFecha(JTextField campo) {
        try {
            return LocalDate.parse(texto(campo));
        } catch (DateTimeParseException errorFecha) {
            throw new IllegalArgumentException("Usa el formato AAAA-MM-DD para las fechas", errorFecha);
        }
    }

    private String ubicacionTexto(EquipoConsulta.UbicacionResumen ubicacion) {
        return ubicacion.sede() + " / " + ubicacion.ambiente() + " / " + ubicacion.area() + " / " + ubicacion.piso();
    }

    private String ubicacionTexto(Movimiento.InstantaneaUbicacion ubicacion) {
        return ubicacion.sede() + " / " + ubicacion.ambiente() + " / " + ubicacion.area() + " / " + ubicacion.piso();
    }

    private String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void cerrarSesion() {
        int opcion = JOptionPane.showConfirmDialog(ventana, "¿Deseas cerrar la sesion?", "Cerrar sesion",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            autenticacion.cerrarSesion();
            JFrame ventanaAnterior = ventana;
            ventana = null;
            ventanaAnterior.dispose();
            if (InicioSesionGUI.mostrar(autenticacion)) {
                new DashboardInventarioGUI(autenticacion, autorizacion, equipos, movimientos, stock, reportes).mostrar();
            }
        }
    }

    private static final class IconoNavegacion implements Icon {
        private final String tipo;

        private IconoNavegacion(String tipo) {
            this.tipo = tipo;
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component componente, Graphics grafico, int x, int y) {
            Graphics2D dibujo = (Graphics2D) grafico.create();
            dibujo.translate(x, y);
            dibujo.setColor(new Color(190, 220, 205));
            dibujo.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (tipo) {
                case "inicio" -> {
                    dibujo.drawLine(2, 8, 9, 2);
                    dibujo.drawLine(9, 2, 16, 8);
                    dibujo.drawRect(4, 8, 10, 8);
                    dibujo.drawRect(8, 11, 3, 5);
                }
                case "equipo", "inventario" -> {
                    dibujo.drawRoundRect(2, 3, 14, 12, 2, 2);
                    dibujo.drawLine(5, 6, 13, 6);
                    dibujo.drawLine(5, 9, 13, 9);
                    dibujo.drawLine(5, 12, 10, 12);
                }
                case "consulta" -> {
                    dibujo.drawOval(2, 2, 10, 10);
                    dibujo.drawLine(10, 10, 16, 16);
                }
                case "lista", "historial", "reporte" -> {
                    dibujo.drawOval(2, 4, 2, 2);
                    dibujo.drawOval(2, 9, 2, 2);
                    dibujo.drawOval(2, 14, 2, 2);
                    dibujo.drawLine(7, 5, 16, 5);
                    dibujo.drawLine(7, 10, 16, 10);
                    dibujo.drawLine(7, 15, 16, 15);
                }
                case "entrada", "salida", "traslado" -> {
                    dibujo.drawLine(3, 6, 15, 6);
                    dibujo.drawLine(3, 12, 15, 12);
                    dibujo.drawLine(12, 3, 15, 6);
                    dibujo.drawLine(12, 9, 15, 12);
                    dibujo.drawLine(3, 6, 6, 9);
                    dibujo.drawLine(3, 12, 6, 15);
                }
                case "ubicacion" -> {
                    dibujo.drawOval(4, 2, 10, 10);
                    dibujo.drawOval(7, 5, 4, 4);
                    dibujo.drawLine(5, 10, 9, 17);
                    dibujo.drawLine(13, 10, 9, 17);
                }
                case "estado" -> {
                    dibujo.drawOval(2, 2, 14, 14);
                    dibujo.drawLine(5, 9, 8, 12);
                    dibujo.drawLine(8, 12, 13, 6);
                }
                case "stock" -> {
                    dibujo.drawRect(3, 9, 3, 7);
                    dibujo.drawRect(8, 5, 3, 11);
                    dibujo.drawRect(13, 2, 3, 14);
                }
                case "alerta" -> {
                    dibujo.drawLine(9, 2, 16, 15);
                    dibujo.drawLine(16, 15, 2, 15);
                    dibujo.drawLine(2, 15, 9, 2);
                    dibujo.drawLine(9, 6, 9, 10);
                    dibujo.fillOval(8, 12, 2, 2);
                }
                case "eliminar" -> {
                    dibujo.drawRect(4, 5, 10, 11);
                    dibujo.drawLine(2, 4, 16, 4);
                    dibujo.drawLine(6, 2, 12, 2);
                    dibujo.drawLine(7, 8, 7, 13);
                    dibujo.drawLine(11, 8, 11, 13);
                }
                case "salir" -> {
                    dibujo.drawRect(2, 2, 9, 14);
                    dibujo.drawLine(8, 9, 16, 9);
                    dibujo.drawLine(13, 6, 16, 9);
                    dibujo.drawLine(13, 12, 16, 9);
                }
                case "aceptar" -> {
                    dibujo.drawLine(3, 9, 7, 13);
                    dibujo.drawLine(7, 13, 15, 4);
                }
                default -> dibujo.drawOval(3, 3, 12, 12);
            }
            dibujo.dispose();
        }
    }

    private record Elemento(UUID id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    private enum Modulo {
        INICIO("Panel principal", "", Permiso.CONSULTAR_INVENTARIO, "inicio"),
        REGISTRAR_EQUIPO("Registrar equipo", "Equipos", Permiso.REGISTRAR_EQUIPOS, "equipo"),
        CONSULTAR_EQUIPO("Consultar equipo", "Equipos", Permiso.CONSULTAR_INVENTARIO, "consulta"),
        LISTAR_EQUIPOS("Listar equipos", "Equipos", Permiso.CONSULTAR_INVENTARIO, "lista"),
        REGISTRAR_INGRESO("Registrar ingreso", "Movimientos", Permiso.REGISTRAR_INGRESOS, "entrada"),
        REGISTRAR_SALIDA("Registrar salida", "Movimientos", Permiso.REGISTRAR_SALIDAS, "salida"),
        TRASLADAR("Trasladar entre sedes", "Movimientos", Permiso.REGISTRAR_TRASLADOS, "traslado"),
        CAMBIAR_UBICACION("Cambiar ubicacion", "Movimientos", Permiso.CAMBIAR_UBICACION_EQUIPO, "ubicacion"),
        CAMBIAR_ESTADO("Cambiar estado tecnico", "Movimientos", Permiso.CAMBIAR_ESTADO_EQUIPO, "estado"),
        CONFIGURAR_MINIMO("Configurar stock minimo", "Stock", Permiso.CONFIGURAR_STOCK_MINIMO, "stock"),
        EXISTENCIAS("Existencias actuales", "Stock", Permiso.CONSULTAR_INVENTARIO, "inventario"),
        ALERTAS("Alertas de stock", "Stock", Permiso.CONSULTAR_ALERTAS, "alerta"),
        HISTORIAL("Historial de movimientos", "Consultas", Permiso.CONSULTAR_HISTORIAL, "historial"),
        REPORTE("Reportes por fechas", "Consultas", Permiso.CONSULTAR_REPORTES, "reporte"),
        ELIMINAR_EQUIPO("Eliminar equipo", "Administracion", Permiso.ELIMINAR_REGISTROS, "eliminar");

        private final String nombre;
        private final String seccion;
        private final Permiso permiso;
        private final String icono;

        Modulo(String nombre, String seccion, Permiso permiso, String icono) {
            this.nombre = nombre;
            this.seccion = seccion;
            this.permiso = permiso;
            this.icono = icono;
        }
    }
}