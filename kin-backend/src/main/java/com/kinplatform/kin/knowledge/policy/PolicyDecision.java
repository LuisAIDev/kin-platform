package com.kinplatform.kin.knowledge.policy;

import java.util.List;

/**
 * Decisión inmutable de una política (especificación Fase 2): veredicto sobre
 * una categoría, motivos auditablemente transportados y, en caso de degradación,
 * la acción de mitigación sugerida.
 *
 * <p>Valor de dominio puro: sin efectos secundarios, sin dependencias de
 * infraestructura. Los motivos permiten trazar cada rechazo o degradación.</p>
 */
public record PolicyDecision(
    PolicyCategory category,
    PolicyVerdict verdict,
    List<String> reasons,
    String action
) {

    public PolicyDecision {
        category = category == null ? PolicyCategory.QUALITY : category;
        verdict = verdict == null ? PolicyVerdict.ALLOW : verdict;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        action = action == null ? "" : action;
    }

    public boolean allowed() {
        return verdict == PolicyVerdict.ALLOW;
    }

    public boolean rejected() {
        return verdict == PolicyVerdict.REJECT;
    }

    public boolean degraded() {
        return verdict == PolicyVerdict.DEGRADE;
    }

    public static PolicyDecision allow(PolicyCategory category) {
        return new PolicyDecision(category, PolicyVerdict.ALLOW, List.of(), "");
    }

    public static PolicyDecision reject(PolicyCategory category, String reason) {
        return new PolicyDecision(category, PolicyVerdict.REJECT,
            List.of(reason == null ? "" : reason), "");
    }

    public static PolicyDecision degrade(PolicyCategory category, String reason, String action) {
        return new PolicyDecision(category, PolicyVerdict.DEGRADE,
            List.of(reason == null ? "" : reason), action == null ? "" : action);
    }
}
