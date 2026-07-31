package com.kinplatform.kin.reporting.opportunity;

import java.util.List;

/**
 * Explicación auditable y evidencia de una oportunidad: qué información se
 * utilizó, qué regla se aplicó, por qué se generó y la evidencia que la
 * sustenta.
 */
public record OpportunityExplanation(
    List<String> usedInformation,
    String appliedRule,
    String reason,
    String evidence
) {

    public OpportunityExplanation {
        usedInformation = usedInformation == null ? List.of() : List.copyOf(usedInformation);
        appliedRule = appliedRule == null ? "" : appliedRule;
        reason = reason == null ? "" : reason;
        evidence = evidence == null ? "" : evidence;
    }

    public static OpportunityExplanation of(List<String> usedInformation, String appliedRule,
                                            String reason, String evidence) {
        return new OpportunityExplanation(usedInformation, appliedRule, reason, evidence);
    }
}
