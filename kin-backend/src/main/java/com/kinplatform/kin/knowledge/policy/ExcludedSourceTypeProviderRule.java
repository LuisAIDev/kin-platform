package com.kinplatform.kin.knowledge.policy;

import java.util.Locale;

/**
 * Regla de proveedores: excluye tipos de fuente configurados (p. ej.
 * {@code social}, {@code forum}). Soporta "solo proveedores gubernamentales"
 * excluyendo los demás tipos vía configuración.
 *
 * <p>Determinista: comparación normalizada contra la configuración inyectada.</p>
 */
public class ExcludedSourceTypeProviderRule implements ProviderRule {

    @Override
    public String name() {
        return "ExcluirTiposDeFuente";
    }

    @Override
    public PolicyDecision evaluate(String sourceType, ProviderPolicyConfig config) {
        if (sourceType == null || sourceType.isBlank()) {
            return PolicyDecision.reject(PolicyCategory.PROVIDER, "Tipo de fuente ausente");
        }
        String normalized = sourceType.trim().toLowerCase(Locale.ROOT);
        if (config.excludedSourceTypes().contains(normalized)) {
            return PolicyDecision.reject(PolicyCategory.PROVIDER,
                "Tipo de fuente excluido: " + normalized);
        }
        return PolicyDecision.allow(PolicyCategory.PROVIDER);
    }
}
