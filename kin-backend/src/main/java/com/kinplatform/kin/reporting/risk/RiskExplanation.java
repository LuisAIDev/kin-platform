package com.kinplatform.kin.reporting.risk;

import java.util.List;

/**
 * Explicación auditable y evidencia de un riesgo: qué información se utilizó,
 * qué regla se aplicó, por qué se generó y la evidencia que lo sustenta.
 */
public record RiskExplanation(
    List<String> usedInformation,
    String appliedRule,
    String reason,
    String evidence
) {

    public RiskExplanation {
        usedInformation = usedInformation == null ? List.of() : List.copyOf(usedInformation);
        appliedRule = appliedRule == null ? "" : appliedRule;
        reason = reason == null ? "" : reason;
        evidence = evidence == null ? "" : evidence;
    }

    public static RiskExplanation of(List<String> usedInformation, String appliedRule,
                                     String reason, String evidence) {
        return new RiskExplanation(usedInformation, appliedRule, reason, evidence);
    }
}
