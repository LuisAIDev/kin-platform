package com.kinplatform.kin.interview;

import com.kinplatform.kin.engine.EngineResult;

/**
 * Resultado inmutable de la evaluación de la entrevista (ADR-015).
 *
 * <p>Contiene la {@link InterviewDecision} (ASK/REPORT), la
 * {@link InterviewDirective} de la pregunta pendiente (si la hay), el
 * {@link InterviewState} resultante y el {@link InterviewProgress}. Implementa
 * {@link EngineResult} para compartir el contrato común de resultados;
 * {@code empty()} permite el modo seguro: sin estado de entrevista, el resultado
 * está vacío y el pipeline no asume entrevista activa.</p>
 */
public record InterviewResult(
    InterviewDecision decision,
    InterviewDirective directive,
    InterviewState state,
    InterviewProgress progress,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public static final String VERSION = "1.0";

    public InterviewResult {
        progress = progress == null ? InterviewProgress.empty() : progress;
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        explanation = explanation == null ? "" : explanation;
        generatedBy = generatedBy == null ? "" : generatedBy;
        engineVersion = engineVersion == null ? "" : engineVersion;
    }

    public static InterviewResult of(InterviewDecision decision, InterviewDirective directive,
                                     InterviewState state, InterviewProgress progress) {
        return new InterviewResult(decision, directive, state, progress,
            1.0, decision != null ? decision.reason() : "", "kin.interview", VERSION);
    }

    public static InterviewResult empty() {
        return new InterviewResult(InterviewDecision.report("Sin entrevista."), null, null,
            InterviewProgress.empty(), 0.0, "No hay datos de entrevista.", "kin.interview", VERSION);
    }

    /**
     * La entrevista está completa (Java habilita la generación del reporte).
     */
    public boolean complete() {
        return state != null && state.complete();
    }

    /**
     * Hay una pregunta de entrevista pendiente de formular.
     */
    public boolean hasDirective() {
        return directive != null;
    }

    @Override
    public boolean isEmpty() {
        return state == null;
    }
}
