package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceTrust;

import java.util.EnumMap;
import java.util.Map;

/**
 * Regla de calidad: rechaza candidatos cuya confianza derivada esté por debajo
 * del umbral mínimo configurado. Complementa a la validación de fuentes
 * (que permanece intacta).
 *
 * <p>Determinista: rango ordinal de confianza del dominio.</p>
 */
public class MinimumTrustQualityRule implements QualityRule {

    private static final Map<SourceTrust, Integer> RANK = rank();

    @Override
    public String name() {
        return "ConfianzaMinima";
    }

    @Override
    public PolicyDecision evaluate(KnowledgeCandidate candidate, SourceTrust trust,
                                   QualityPolicyConfig config) {
        if (RANK.getOrDefault(trust, 0) < RANK.getOrDefault(config.minTrust(), 0)) {
            return PolicyDecision.reject(PolicyCategory.QUALITY,
                "Confianza mínima requerida: " + config.minTrust().displayName()
                    + "; obtenida: " + trust.displayName());
        }
        return PolicyDecision.allow(PolicyCategory.QUALITY);
    }

    private static Map<SourceTrust, Integer> rank() {
        var rank = new EnumMap<SourceTrust, Integer>(SourceTrust.class);
        rank.put(SourceTrust.UNVERIFIED, 0);
        rank.put(SourceTrust.SECONDARY, 1);
        rank.put(SourceTrust.OFFICIAL_PUBLIC, 2);
        return Map.copyOf(rank);
    }
}
