package com.computototal.inventario.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.computototal.inventario.servicio.AutenticacionServicio;

public final class InicioSesionGUI {
    private static final Color PRIMARIO_COLOR = new Color(24, 64, 57);
    private static final Color SECUNDARIO_COLOR = new Color(28, 112, 88);
    private static final Color TEXTO = new Color(35, 48, 45);
    private static final Color TEXTO_SECUNDARIO = new Color(103, 117, 112);

    private InicioSesionGUI() {
    }

    public static boolean mostrar(AutenticacionServicio autenticacion) {
        Objects.requireNonNull(autenticacion, "autenticacion");
        AtomicBoolean sesionIniciada = new AtomicBoolean();
        Runnable mostrarDialogo = () -> mostrarDialogo(autenticacion, sesionIniciada);
        if (SwingUtilities.isEventDispatchThread()) {
            mostrarDialogo.run();
            return sesionIniciada.get();
        }
        try {
            SwingUtilities.invokeAndWait(mostrarDialogo);
        } catch (InterruptedException errorInterrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpio el inicio de sesion", errorInterrupcion);
        } catch (InvocationTargetException errorSwing) {
            throw new IllegalStateException("No se pudo abrir la ventana de inicio de sesion", errorSwing.getCause());
        }
        return sesionIniciada.get();
    }

    private static void mostrarDialogo(AutenticacionServicio autenticacion, AtomicBoolean sesionIniciada) {
        JDialog dialogo = new JDialog((java.awt.Frame) null, "Inicio de sesion", Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        GradientPanel fondo = new GradientPanel();
        fondo.setLayout(new GridBagLayout());
        fondo.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel formulario = new JPanel();
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        formulario.setLayout(new javax.swing.BoxLayout(formulario, javax.swing.BoxLayout.Y_AXIS));

        JLabel marca = new JLabel("COMPUTO TOTAL  /  GMD");
        marca.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        marca.setForeground(SECUNDARIO_COLOR);
        marca.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        JLabel titulo = new JLabel("Acceso al inventario");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        titulo.setForeground(TEXTO);
        titulo.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        JLabel subtitulo = new JLabel("Ingresa tus credenciales para continuar.");
        subtitulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitulo.setForeground(TEXTO_SECUNDARIO);
        subtitulo.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        JTextField usuario = crearCampoTexto();
        JPasswordField contrasena = new JPasswordField();
        estilizarCampo(contrasena);
        JLabel mensaje = new JLabel(" ");
        mensaje.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        mensaje.setForeground(new Color(176, 55, 48));
        mensaje.setPreferredSize(new Dimension(320, 24));
        mensaje.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        JButton ingresar = new JButton("Iniciar sesion");
        ingresar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        ingresar.setForeground(Color.WHITE);
        ingresar.setBackground(SECUNDARIO_COLOR);
        ingresar.setFocusPainted(false);
        ingresar.setBorderPainted(false);
        ingresar.setPreferredSize(new Dimension(320, 44));
        ingresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        ingresar.setAlignmentX(JButton.LEFT_ALIGNMENT);

        JButton cancelar = new JButton("Cerrar");
        cancelar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        cancelar.setForeground(TEXTO_SECUNDARIO);
        cancelar.setContentAreaFilled(false);
        cancelar.setBorderPainted(false);
        cancelar.setFocusPainted(false);
        cancelar.setAlignmentX(JButton.CENTER_ALIGNMENT);

        formulario.add(marca);
        formulario.add(javax.swing.Box.createVerticalStrut(18));
        formulario.add(titulo);
        formulario.add(javax.swing.Box.createVerticalStrut(6));
        formulario.add(subtitulo);
        formulario.add(javax.swing.Box.createVerticalStrut(25));
        agregarCampo(formulario, "Usuario", usuario);
        agregarCampo(formulario, "Contrasena", contrasena);
        formulario.add(mensaje);
        formulario.add(ingresar);
        formulario.add(javax.swing.Box.createVerticalStrut(10));
        formulario.add(cancelar);

        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        restricciones.weightx = 1;
        fondo.add(formulario, restricciones);
        dialogo.add(fondo, BorderLayout.CENTER);

        ingresar.addActionListener(evento -> {
            String nombreUsuario = usuario.getText().strip();
            char[] clave = contrasena.getPassword();
            if (nombreUsuario.isEmpty() || clave.length == 0) {
                Arrays.fill(clave, '\0');
                mensaje.setText("Ingresa el usuario y la contrasena.");
                if (nombreUsuario.isEmpty()) {
                    usuario.requestFocusInWindow();
                } else {
                    contrasena.requestFocusInWindow();
                }
                return;
            }
            try {
                autenticacion.iniciarSesion(nombreUsuario, clave);
                sesionIniciada.set(true);
                dialogo.dispose();
            } catch (SecurityException errorAcceso) {
                mensaje.setText("Usuario o contrasena incorrectos.");
                contrasena.setText("");
                contrasena.requestFocusInWindow();
            } catch (RuntimeException errorAcceso) {
                mensaje.setText("No se pudo validar el acceso. Intentalo nuevamente.");
                contrasena.setText("");
                contrasena.requestFocusInWindow();
            }
        });
        cancelar.addActionListener(evento -> dialogo.dispose());
        dialogo.getRootPane().setDefaultButton(ingresar);
        dialogo.setPreferredSize(new Dimension(500, 510));
        dialogo.setResizable(false);
        dialogo.pack();
        dialogo.setLocationRelativeTo(null);
        usuario.requestFocusInWindow();
        dialogo.setVisible(true);
    }

    private static JTextField crearCampoTexto() {
        JTextField campo = new JTextField();
        estilizarCampo(campo);
        return campo;
    }

    private static void estilizarCampo(JTextField campo) {
        campo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        campo.setForeground(TEXTO);
        campo.setPreferredSize(new Dimension(320, 42));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(213, 221, 218)),
                BorderFactory.createEmptyBorder(8, 11, 8, 11)));
        campo.setAlignmentX(JTextField.LEFT_ALIGNMENT);
    }

    private static void agregarCampo(JPanel formulario, String etiqueta, JTextField campo) {
        JLabel tituloCampo = new JLabel(etiqueta);
        tituloCampo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        tituloCampo.setForeground(TEXTO);
        tituloCampo.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        formulario.add(tituloCampo);
        formulario.add(javax.swing.Box.createVerticalStrut(7));
        formulario.add(campo);
        formulario.add(javax.swing.Box.createVerticalStrut(16));
    }

    private static final class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics grafico) {
            Graphics2D contexto = (Graphics2D) grafico.create();
            contexto.setPaint(new GradientPaint(0, 0, PRIMARIO_COLOR, getWidth(), getHeight(), SECUNDARIO_COLOR));
            contexto.fillRect(0, 0, getWidth(), getHeight());
            contexto.dispose();
        }
    }
}