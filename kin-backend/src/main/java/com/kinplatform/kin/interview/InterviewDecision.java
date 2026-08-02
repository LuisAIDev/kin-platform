package com.kinplatform.kin.interview;

/**
 * Decisión determinista del flujo de la entrevista (ADR-015).
 *
 * <p>{@code ASK} indica que la entrevista continúa con la pregunta identificada
 * por {@code questionId}; {@code REPORT} indica que la entrevista está completa
 * y Java habilita la generación del {@code ConsultingReport}. Esta decisión se
 * toma 100 % en Java; el LLM únicamente comunica.</p>
 */
public record InterviewDecision(
    Action action,
    String questionId,
    String reason
) {

    public enum Action {
        ASK,
        REPORT
    }

    public InterviewDecision {
        if (action == null) {
            throw new IllegalArgumentException("action no puede ser null");
        }
        questionId = questionId == null || questionId.isBlank() ? null : questionId;
        reason = reason == null ? "" : reason;
    }

    public static InterviewDecision ask(String questionId, String reason) {
        return new InterviewDecision(Action.ASK, questionId, reason);
    }

    public static InterviewDecision report(String reason) {
        return new InterviewDecision(Action.REPORT, null, reason);
    }

    public boolean isAsk() {
        return action == Action.ASK;
    }

    public boolean isReport() {
        return action == Action.REPORT;
    }
}
