package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.engine.SourceValidator;

import java.util.Locale;

/**
 * Regla de calidad: restringe el tipo de fuente del candidato a los permitidos
 * (p. ej. solo fuentes gubernamentales). Reutiliza la clave congelada de tipo
 * de fuente de la validación. Un tipo ausente se degrada, no se rechaza.
 *
 * <p>Determinista: lectura de {@code meta[source_type]}.</p>
 */
public class AllowedSourceTypeQualityRule implements QualityRule {

    @Override
    public String name() {
        return "TipoDeFuentePermitido";
    }

    @Override
    public PolicyDecision evaluate(KnowledgeCandidate candidate, SourceTrust trust,
                                   QualityPolicyConfig config) {
        if (config.allowedSourceTypes().isEmpty()) {
            return PolicyDecision.allow(PolicyCategory.QUALITY);
        }
        String sourceType = candidate.meta().get(SourceValidator.META_SOURCE_TYPE);
        if (sourceType == null || sourceType.isBlank()) {
            return PolicyDecision.degrade(PolicyCategory.QUALITY,
                "Tipo de fuente no declarado", "ignorar_tipo_fuente");
        }
        String normalized = sourceType.trim().toLowerCase(Locale.ROOT);
        if (!config.allowedSourceTypes().contains(normalized)) {
            return PolicyDecision.reject(PolicyCategory.QUALITY, "Tipo de fuente no permitido: " + sourceType);
        }
        return PolicyDecision.allow(PolicyCategory.QUALITY);
    }
}
