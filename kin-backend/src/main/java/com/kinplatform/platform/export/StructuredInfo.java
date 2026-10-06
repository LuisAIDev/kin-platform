package com.kinplatform.platform.export;

/**
 * Dato estructurado del proyecto proveniente de {@code project_info}.
 *
 * @param section    sección de la información estructurada (p. ej. MERCADO)
 * @param key        clave dentro de la sección (p. ej. mercado_objetivo)
 * @param value      valor almacenado
 * @param sourceType tipo de fuente (USER_INPUT, IMPORTED_DOCUMENT, ...)
 */
public record StructuredInfo(String section, String key, String value, String sourceType) {

    public StructuredInfo {
        section = section == null ? "" : section;
        key = key == null ? "" : key;
        value = value == null ? "" : value;
        sourceType = sourceType == null ? "" : sourceType;
    }
}

