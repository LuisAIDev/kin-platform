package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceTrust;

import java.util.Locale;

/**
 * Regla de calidad: restringe el idioma del candidato a los permitidos.
 * Si no hay idiomas configurados, no restringe. Un idioma ausente se degrada
 * (no se rechaza): la ausencia de idioma no invalida el dato.
 *
 * <p>Determinista: lectura de {@code meta[language]} (clave aditiva
 * {@link PolicyKeys#META_LANGUAGE}).</p>
 */
public class AllowedLanguageQualityRule implements QualityRule {

    @Override
    public String name() {
        return "IdiomaPermitido";
    }

    @Override
    public PolicyDecision evaluate(KnowledgeCandidate candidate, SourceTrust trust,
                                   QualityPolicyConfig config) {
        if (config.allowedLanguages().isEmpty()) {
            return PolicyDecision.allow(PolicyCategory.QUALITY);
        }
        String language = candidate.meta().get(PolicyKeys.META_LANGUAGE);
        if (language == null || language.isBlank()) {
            return PolicyDecision.degrade(PolicyCategory.QUALITY,
                "Idioma no declarado", "ignorar_idioma");
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (!config.allowedLanguages().contains(normalized)) {
            return PolicyDecision.reject(PolicyCategory.QUALITY, "Idioma no permitido: " + language);
        }
        return PolicyDecision.allow(PolicyCategory.QUALITY);
    }
}
