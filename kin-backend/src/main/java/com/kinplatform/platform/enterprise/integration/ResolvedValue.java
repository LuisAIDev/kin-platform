package com.kinplatform.platform.enterprise.integration;

import com.kinplatform.platform.projectinfo.StructuredInfoSourceType;

/**
 * Valor resuelto de un dato con su origen y estado explícito.
 *
 * <p>{@code value} puede ser {@code null} cuando el estado es {@code PENDING}
 * o {@code NOT_AVAILABLE}: la ausencia de dato NUNCA se representa como
 * {@code 0}.</p>
 */
public record ResolvedValue(String value, StructuredInfoSourceType sourceType, DataState state, String origin) {

    /** Dato todavía no proporcionado. */
    public static ResolvedValue pending() {
        return new ResolvedValue(null, null, DataState.PENDING, null);
    }

    /** Dato no disponible o no calculable con los datos actuales. */
    public static ResolvedValue notAvailable() {
        return new ResolvedValue(null, null, DataState.NOT_AVAILABLE, null);
    }

    /** Deriva el estado de presentación a partir del origen del dato. */
    public static ResolvedValue of(String value, StructuredInfoSourceType sourceType, String origin) {
        if (value == null || value.isBlank()) {
            return pending();
        }
        DataState state =
                switch (sourceType) {
                    case USER_INPUT -> DataState.CONFIRMED;
                    case IMPORTED_DOCUMENT -> DataState.IMPORTED;
                    case CALCULATED -> DataState.CALCULATED;
                    case ESTIMATED -> DataState.ESTIMATED;
                    case AI_SUGGESTED -> DataState.AI_SUGGESTED;
                };
        return new ResolvedValue(value, sourceType, state, origin);
    }
}


