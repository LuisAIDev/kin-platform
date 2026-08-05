package com.kinplatform.kin.knowledge.policy;

/**
 * Claves adicionales de {@code meta} de un candidato consumidas por el
 * Knowledge Policy Engine (especificación Fase 2). Son aditivas: no redefinen
 * ni reemplazan las claves congeladas de la validación de fuentes.
 */
public final class PolicyKeys {

    /** Idioma declarado del candidato (p. ej. {@code es}, {@code en}). */
    public static final String META_LANGUAGE = "language";

    /** Licencia declarada del candidato (p. ej. {@code cc-by}, {@code public}). */
    public static final String META_LICENSE = "license";

    private PolicyKeys() {
    }
}
