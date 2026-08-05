package com.kinplatform.kin.knowledge.policy;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Configuración de políticas de proveedores (especificación Fase 2): tipos de
 * fuente excluidos, límite máximo de proveedores por turno y prioridades por
 * tipo de fuente (mayor valor = mayor prioridad).
 *
 * <p>Inmutable y determinista. Los tipos de fuente son cadenas abstractas del
 * dominio (p. ej. {@code government}, {@code web_search}, {@code document},
 * {@code vector_rag}): el motor de políticas nunca conoce adaptadores.</p>
 */
public record ProviderPolicyConfig(
    Set<String> excludedSourceTypes,
    int maxProviders,
    Map<String, Integer> priorities
) {

    public ProviderPolicyConfig {
        excludedSourceTypes = lowerCaseSet(excludedSourceTypes);
        maxProviders = Math.max(0, maxProviders);
        priorities = priorities == null ? Map.of() : normalizePriorities(priorities);
    }

    public static ProviderPolicyConfig defaults() {
        return new ProviderPolicyConfig(Set.of(), 5,
            Map.of("government", 100, "statistics", 90, "internal_db", 80,
                "vector_rag", 70, "document", 60, "web_search", 40));
    }

    public static ProviderPolicyConfig dev() {
        return new ProviderPolicyConfig(Set.of(), 8,
            Map.of("government", 100, "statistics", 90, "internal_db", 80,
                "vector_rag", 70, "document", 60, "web_search", 40));
    }

    public static ProviderPolicyConfig production() {
        return new ProviderPolicyConfig(Set.of("social", "forum"), 5,
            Map.of("government", 100, "statistics", 90, "internal_db", 80,
                "vector_rag", 70, "document", 60, "web_search", 40));
    }

    public static ProviderPolicyConfig testing() {
        return new ProviderPolicyConfig(Set.of("social", "forum"), 2,
            Map.of("government", 100, "web_search", 40));
    }

    public static ProviderPolicyConfig enterprise() {
        return new ProviderPolicyConfig(Set.of("social", "forum"), 10,
            Map.of("government", 100, "statistics", 90, "internal_db", 80,
                "vector_rag", 70, "document", 60, "web_search", 40));
    }

    public int priorityOf(String sourceType) {
        return priorities.getOrDefault(lower(sourceType), 0);
    }

    private static Set<String> lowerCaseSet(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        var out = new LinkedHashSet<String>();
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(out);
    }

    private static Map<String, Integer> normalizePriorities(Map<String, Integer> values) {
        var out = new LinkedHashMap<String, Integer>();
        for (var entry : values.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isBlank()) {
                out.put(key.trim().toLowerCase(Locale.ROOT),
                    entry.getValue() == null ? 0 : entry.getValue());
            }
        }
        return Map.copyOf(out);
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
