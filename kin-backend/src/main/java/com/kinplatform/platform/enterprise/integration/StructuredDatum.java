package com.kinplatform.platform.enterprise.integration;

import com.kinplatform.projectinfo.StructuredInfoSourceType;

/**
 * Entrada de información estructurada del proyecto en la capa de integración.
 *
 * <p>Representación neutral de una fila de {@code project_structured_info}
 * (sección, clave, valor y origen). El adaptador de infraestructura la
 * construye desde la entidad JPA; la capa de dominio no conoce la
 * persistencia.</p>
 */
public record StructuredDatum(String section, String key, String value, StructuredInfoSourceType sourceType) {

    public StructuredDatum {
        if (section == null || section.isBlank()) {
            throw new IllegalArgumentException("'section' no puede ser null o vacía.");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("'key' no puede ser null o vacía.");
        }
        if (value == null) {
            throw new IllegalArgumentException("'value' no puede ser null.");
        }
        if (sourceType == null) {
            throw new IllegalArgumentException("'sourceType' no puede ser null.");
        }
    }

    /** Un dato no proporcionado se representa como PENDING, nunca como 0. */
    public boolean hasValue() {
        return !value.isBlank();
    }
}

