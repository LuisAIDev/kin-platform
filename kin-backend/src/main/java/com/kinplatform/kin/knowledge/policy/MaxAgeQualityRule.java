package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceTrust;

import java.time.OffsetDateTime;

/**
 * Regla de calidad: rechaza candidatos cuya fecha de publicación supere la
 * antigüedad máxima configurada. La ausencia de fecha ya la gestiona la
 * validación de fuentes; esta regla solo añade el umbral configurable.
 *
 * <p>Determinista: comparación temporal contra la configuración inyectada.</p>
 */
public class MaxAgeQualityRule implements QualityRule {

    @Override
    public String name() {
        return "AntiguedadMaxima";
    }

    @Override
    public PolicyDecision evaluate(KnowledgeCandidate candidate, SourceTrust trust,
                                   QualityPolicyConfig config) {
        if (candidate.publishedAt() == null) {
            return PolicyDecision.allow(PolicyCategory.QUALITY);
        }
        if (candidate.publishedAt().isBefore(OffsetDateTime.now().minus(config.maxAge()))) {
            return PolicyDecision.reject(PolicyCategory.QUALITY,
                "Fuera de la ventana de antigüedad máxima: " + candidate.publishedAt());
        }
        return PolicyDecision.allow(PolicyCategory.QUALITY);
    }
}
