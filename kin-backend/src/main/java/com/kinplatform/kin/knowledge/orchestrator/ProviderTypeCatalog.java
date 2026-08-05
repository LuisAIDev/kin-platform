package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.planner.ProviderType;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo determinista ProviderType → tipo de fuente abstracto (cadena de
 * dominio usada por las políticas de proveedores). Evita duplicación y mantiene
 * el contrato de que el orquestador jamás nombra proveedores concretos.
 */
public final class ProviderTypeCatalog {

    private static final Map<ProviderType, String> SOURCE_TYPES = Map.ofEntries(
        Map.entry(ProviderType.GOVERNMENT, "government"),
        Map.entry(ProviderType.STATISTICS, "statistics"),
        Map.entry(ProviderType.WEB_SEARCH, "web_search"),
        Map.entry(ProviderType.DOCUMENT, "document"),
        Map.entry(ProviderType.VECTOR_RAG, "vector_rag"),
        Map.entry(ProviderType.INTERNAL_DB, "internal_db"));

    private static final Map<String, ProviderType> REVERSE = buildReverse();

    private ProviderTypeCatalog() {
    }

    public static String sourceType(ProviderType type) {
        if (type == null) {
            return "";
        }
        return SOURCE_TYPES.getOrDefault(type, type.name().toLowerCase(Locale.ROOT));
    }

    public static Optional<ProviderType> fromSourceType(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(REVERSE.get(value.trim().toLowerCase(Locale.ROOT)));
    }

    private static Map<String, ProviderType> buildReverse() {
        var reverse = new LinkedHashMap<String, ProviderType>();
        for (var entry : SOURCE_TYPES.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }
        return Map.copyOf(reverse);
    }
}
