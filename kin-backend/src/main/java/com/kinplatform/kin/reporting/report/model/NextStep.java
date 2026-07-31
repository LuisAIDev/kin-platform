package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.engine.DeterministicId;

import java.util.UUID;

/**
 * Paso siguiente sugerido, agregado de los top items de recomendaciones,
 * riesgos y oportunidades. Solo etiqueta valores ya existentes: no calcula
 * nada nuevo.
 */
public record NextStep(
    UUID id,
    String source,
    String title,
    int priority,
    String reason
) {

    public static final String SOURCE_RECOMMENDATION = "RECOMMENDATION";
    public static final String SOURCE_RISK_MITIGATION = "RISK_MITIGATION";
    public static final String SOURCE_OPPORTUNITY = "OPPORTUNITY";

    public NextStep {
        source = source == null ? "" : source;
        title = title == null ? "" : title;
        priority = Math.max(1, Math.min(10, priority));
        reason = reason == null ? "" : reason;
        id = id == null ? DeterministicId.from(source, title, reason) : id;
    }

    public static NextStep of(String source, String title, int priority, String reason) {
        return new NextStep(null, source, title, priority, reason);
    }
}
