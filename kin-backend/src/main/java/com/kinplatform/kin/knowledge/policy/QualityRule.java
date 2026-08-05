package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceTrust;

/**
 * Contrato tipado de una regla de política de calidad (Strategy Pattern):
 * evalúa un candidato (y la confianza ya derivada por la validación de fuentes)
 * contra la configuración. Se compone después de la validación de fuentes;
 * nunca la sustituye.
 */
public interface QualityRule extends PolicyRule {

    @Override
    default PolicyCategory category() {
        return PolicyCategory.QUALITY;
    }

    PolicyDecision evaluate(KnowledgeCandidate candidate, SourceTrust trust, QualityPolicyConfig config);
}
