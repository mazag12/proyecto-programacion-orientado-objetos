package com.computototal.inventario.servicio;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class GestorContrasenas {
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 600_000;
    private static final int LONGITUD_HASH_BITS = 256;
    private static final int LONGITUD_SAL_BYTES = 16;

    private final SecureRandom generadorAleatorio;

    public GestorContrasenas() {
        this(new SecureRandom());
    }

    GestorContrasenas(SecureRandom generadorAleatorio) {
        this.generadorAleatorio = Objects.requireNonNull(generadorAleatorio, "generadorAleatorio");
    }

    public Credencial generarHash(char[] contrasena) {
        Objects.requireNonNull(contrasena, "contrasena");
        byte[] sal = new byte[LONGITUD_SAL_BYTES];
        byte[] hash = null;
        PBEKeySpec especificacion = null;
        generadorAleatorio.nextBytes(sal);
        try {
            especificacion = new PBEKeySpec(contrasena, sal, ITERACIONES, LONGITUD_HASH_BITS);
            hash = SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
            return new Credencial(hash, sal);
        } catch (GeneralSecurityException excepcion) {
            throw new IllegalStateException("No se pudo generar la credencial", excepcion);
        } finally {
            if (especificacion != null) {
                especificacion.clearPassword();
            }
            Arrays.fill(contrasena, '\0');
            Arrays.fill(sal, (byte) 0);
            if (hash != null) {
                Arrays.fill(hash, (byte) 0);
            }
        }
    }

    public boolean verificar(char[] contrasena, byte[] hashEsperado, byte[] sal) {
        Objects.requireNonNull(contrasena, "contrasena");
        byte[] hashCopia = hashEsperado == null ? null : hashEsperado.clone();
        byte[] salCopia = sal == null ? null : sal.clone();
        byte[] hashCalculado = null;
        PBEKeySpec especificacion = null;
        try {
            if (hashCopia == null || hashCopia.length == 0 || salCopia == null || salCopia.length == 0) {
                return false;
            }
            especificacion = new PBEKeySpec(contrasena, salCopia, ITERACIONES, LONGITUD_HASH_BITS);
            hashCalculado = SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
            return MessageDigest.isEqual(hashCalculado, hashCopia);
        } catch (GeneralSecurityException excepcion) {
            throw new IllegalStateException("No se pudo verificar la credencial", excepcion);
        } finally {
            if (especificacion != null) {
                especificacion.clearPassword();
            }
            Arrays.fill(contrasena, '\0');
            if (hashCopia != null) {
                Arrays.fill(hashCopia, (byte) 0);
            }
            if (salCopia != null) {
                Arrays.fill(salCopia, (byte) 0);
            }
            if (hashCalculado != null) {
                Arrays.fill(hashCalculado, (byte) 0);
            }
        }
    }

    public static final class Credencial {
        private final byte[] hashContrasena;
        private final byte[] salContrasena;

        private Credencial(byte[] hashContrasena, byte[] salContrasena) {
            this.hashContrasena = Objects.requireNonNull(hashContrasena, "hashContrasena").clone();
            this.salContrasena = Objects.requireNonNull(salContrasena, "salContrasena").clone();
        }

        public byte[] getHashContrasena() {
            return hashContrasena.clone();
        }

        public byte[] getSalContrasena() {
            return salContrasena.clone();
        }
    }
}