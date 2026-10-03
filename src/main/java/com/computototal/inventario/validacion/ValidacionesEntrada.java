package com.computototal.inventario.validacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ValidacionesEntrada {
    private static final Pattern FORMATO_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern FORMATO_TELEFONO = Pattern.compile("^\\+?[0-9() -]+$");
    private static final Pattern FORMATO_NUMERO_SERIE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/-]{1,38}[A-Za-z0-9]$");

    private ValidacionesEntrada() {
    }

    public static String textoObligatorio(String valor, String nombreCampo) {
        Objects.requireNonNull(valor, nombreCampo + " no puede ser null");
        String normalizado = valor.strip();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacio");
        }
        if (normalizado.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(nombreCampo + " contiene caracteres no permitidos");
        }
        return normalizado;
    }

    public static String texto(String valor, String nombreCampo, int longitudMinima, int longitudMaxima) {
        if (longitudMinima < 0 || longitudMaxima < longitudMinima) {
            throw new IllegalArgumentException("El rango de longitud configurado no es valido");
        }
        String normalizado = textoObligatorio(valor, nombreCampo);
        if (normalizado.length() < longitudMinima || normalizado.length() > longitudMaxima) {
            throw new IllegalArgumentException(nombreCampo + " debe tener entre " + longitudMinima
                    + " y " + longitudMaxima + " caracteres");
        }
        return normalizado;
    }

    public static String textoClave(String valor, String nombreCampo) {
        return textoObligatorio(valor, nombreCampo).toLowerCase(Locale.ROOT);
    }

    public static String numeroSerie(String valor) {
        String normalizado = texto(valor, "numeroSerie", 3, 40);
        if (!FORMATO_NUMERO_SERIE.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("numeroSerie solo admite letras, numeros, guion, punto, guion bajo o barra; debe iniciar y terminar con letra o numero");
        }
        return normalizado.toUpperCase(Locale.ROOT);
    }

    public static UUID identificador(UUID valor, String nombreCampo) {
        return Objects.requireNonNull(valor, nombreCampo + " no puede ser null");
    }

    public static <T> T requerido(T valor, String nombreCampo) {
        return Objects.requireNonNull(valor, nombreCampo + " no puede ser null");
    }

    public static void contrasena(char[] valor) {
        if (valor == null || valor.length == 0) {
            throw new IllegalArgumentException("La contrasena no puede estar vacia");
        }
    }

    public static int entero(String valor, String nombreCampo, int minimo, int maximo) {
        String normalizado = textoObligatorio(valor, nombreCampo);
        final int numero;
        try {
            numero = Integer.parseInt(normalizado);
        } catch (NumberFormatException errorNumero) {
            throw new IllegalArgumentException(nombreCampo + " debe ser un numero entero", errorNumero);
        }
        if (numero < minimo || numero > maximo) {
            throw new IllegalArgumentException(nombreCampo + " debe estar entre " + minimo + " y " + maximo);
        }
        return numero;
    }

    public static BigDecimal decimal(String valor, String nombreCampo, BigDecimal minimo, BigDecimal maximo) {
        String normalizado = textoObligatorio(valor, nombreCampo);
        final BigDecimal numero;
        try {
            numero = new BigDecimal(normalizado);
        } catch (NumberFormatException errorNumero) {
            throw new IllegalArgumentException(nombreCampo + " debe ser un numero valido", errorNumero);
        }
        if ((minimo != null && numero.compareTo(minimo) < 0)
                || (maximo != null && numero.compareTo(maximo) > 0)) {
            throw new IllegalArgumentException(nombreCampo + " esta fuera del rango permitido");
        }
        return numero;
    }

    public static LocalDate fecha(String valor, String nombreCampo) {
        try {
            return LocalDate.parse(textoObligatorio(valor, nombreCampo));
        } catch (DateTimeParseException errorFecha) {
            throw new IllegalArgumentException(nombreCampo + " debe tener una fecha valida con formato AAAA-MM-DD",
                    errorFecha);
        }
    }

    public static void rangoFechas(LocalDate inicio, LocalDate fin) {
        Objects.requireNonNull(inicio, "La fecha inicial no puede ser null");
        Objects.requireNonNull(fin, "La fecha final no puede ser null");
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
    }

    public static String telefono(String valor) {
        String normalizado = textoObligatorio(valor, "telefono");
        long digitos = normalizado.chars().filter(Character::isDigit).count();
        if (!FORMATO_TELEFONO.matcher(normalizado).matches() || digitos < 7 || digitos > 15) {
            throw new IllegalArgumentException("telefono debe contener entre 7 y 15 digitos y solo usar +, espacios, parentesis o guiones");
        }
        return normalizado;
    }

    public static String correoElectronico(String valor) {
        String normalizado = textoObligatorio(valor, "correoElectronico");
        if (!FORMATO_CORREO.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("correoElectronico no tiene un formato valido");
        }
        return normalizado;
    }
}