package com.kinplatform.kin.knowledge.policy;

import java.util.List;

/**
 * Selección inmutable de proveedores (especificación Fase 2): tipos de fuente
 * permitidos (ordenados por prioridad y acotados al límite configurado), tipos
 * rechazados y los motivos de cada decisión.
 */
public record ProviderSelection(
    List<String> allowedTypes,
    List<String> rejectedTypes,
    List<String> reasons,
    int limit
) {

    public ProviderSelection {
        allowedTypes = allowedTypes == null ? List.of() : List.copyOf(allowedTypes);
        rejectedTypes = rejectedTypes == null ? List.of() : List.copyOf(rejectedTypes);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        limit = Math.max(1, limit);
    }

    public boolean isEmpty() {
        return allowedTypes.isEmpty();
    }

    public static ProviderSelection of(List<String> allowedTypes, List<String> rejectedTypes,
                                       List<String> reasons, int limit) {
        return new ProviderSelection(allowedTypes, rejectedTypes, reasons, limit);
    }
}
