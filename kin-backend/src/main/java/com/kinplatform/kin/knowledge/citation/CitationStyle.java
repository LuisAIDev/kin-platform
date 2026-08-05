package com.kinplatform.kin.knowledge.citation;

/**
 * Estilos de citación (Fase 5 — Citation Engine): estrategias registrables que
 * el motor soporta sin codificar estilos directamente (Strategy Pattern).
 */
public enum CitationStyle {

    INLINE("En línea"),
    FOOTNOTE("Nota al pie"),
    APPENDIX("Apéndice"),
    HIDDEN("Oculto"),
    DISABLED("Deshabilitado");

    private final String displayName;

    CitationStyle(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
