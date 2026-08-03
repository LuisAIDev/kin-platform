package com.kinplatform.kin.enterprise.valueobjects;

import java.util.List;

/**
 * Utilidades de validación compartidas por los value objects del módulo
 * enterprise (paquete {@code valueobjects}).
 *
 * <p>Centraliza las comprobaciones de invariantes más habituales (nulidad,
 * strings en blanco, rangos numéricos y colecciones inmutables) para que cada
 * value object las use desde su constructor compacto sin duplicar lógica.
 * Clase de paquete: no forma parte del API público del dominio.</p>
 */
final class ValueObjects {

    private ValueObjects() {
    }

    /**
     * Rechaza strings nulos o en blanco.
     */
    static String requireNotBlank(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null.");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("'" + field + "' no puede ser vacío o contener solo espacios.");
        }
        return value;
    }

    /**
     * Valida una lista de strings sin elementos nulos ni en blanco y la
     * devuelve inmutable ({@link List#copyOf}).
     */
    static List<String> immutableNotBlank(List<String> values, String field) {
        if (values == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null.");
        }
        for (String value : values) {
            if (value == null) {
                throw new IllegalArgumentException("'" + field + "' no puede contener elementos null.");
            }
            if (value.isBlank()) {
                throw new IllegalArgumentException("'" + field + "' no puede contener strings vacíos.");
            }
        }
        return List.copyOf(values);
    }

    /**
     * Valida una lista sin elementos nulos y la devuelve inmutable.
     */
    static <T> List<T> immutableNonNull(List<T> values, String field) {
        if (values == null) {
            throw new IllegalArgumentException("'" + field + "' no puede ser null.");
        }
        for (T value : values) {
            if (value == null) {
                throw new IllegalArgumentException("'" + field + "' no puede contener elementos null.");
            }
        }
        return List.copyOf(values);
    }

    /**
     * Rechaza enteros fuera de {@code [min, max]}.
     */
    static int requireInRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("'" + field + "' debe estar entre " + min + " y "
                + max + " (recibido: " + value + ").");
        }
        return value;
    }

    /**
     * Rechaza reales fuera de {@code [min, max]}.
     */
    static double requireInRange(double value, double min, double max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("'" + field + "' debe estar entre " + min + " y "
                + max + " (recibido: " + value + ").");
        }
        return value;
    }

    /**
     * Rechaza enteros negativos.
     */
    static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException("'" + field + "' no puede ser negativo (recibido: " + value + ").");
        }
        return value;
    }

    /**
     * Rechaza reales negativos.
     */
    static double requireNonNegative(double value, String field) {
        if (value < 0.0) {
            throw new IllegalArgumentException("'" + field + "' no puede ser negativo (recibido: " + value + ").");
        }
        return value;
    }
}
