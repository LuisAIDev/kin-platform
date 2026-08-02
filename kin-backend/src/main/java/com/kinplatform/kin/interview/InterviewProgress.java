package com.kinplatform.kin.interview;

/**
 * Instantánea inmutable del progreso de la entrevista (ADR-015).
 *
 * <p>Derivable de {@link InterviewState} ({@code from}) y pensada para
 * observabilidad y para el reporte de la información faltante. Todos los
 * contadores se acotan a valores no negativos y {@code completenessRatio} se
 * acota al intervalo [0, 1].</p>
 */
public record InterviewProgress(
    int answeredCount,
    int pendingCount,
    int totalQuestions,
    int exchangeUsed,
    int exchangeBudget,
    boolean complete
) {

    public InterviewProgress {
        answeredCount = Math.max(0, answeredCount);
        pendingCount = Math.max(0, pendingCount);
        totalQuestions = Math.max(0, totalQuestions);
        exchangeUsed = Math.max(0, exchangeUsed);
        exchangeBudget = Math.max(0, exchangeBudget);
    }

    public static InterviewProgress of(int answeredCount, int pendingCount, int totalQuestions,
                                       int exchangeUsed, int exchangeBudget, boolean complete) {
        return new InterviewProgress(answeredCount, pendingCount, totalQuestions, exchangeUsed, exchangeBudget, complete);
    }

    public static InterviewProgress from(InterviewState state, int totalQuestions) {
        return state.toProgress(totalQuestions);
    }

    public static InterviewProgress empty() {
        return new InterviewProgress(0, 0, 0, 0, 0, false);
    }

    /**
     * Fracción de preguntas respondidas sobre el total planificado, acotada a
     * [0, 1]; 0.0 si no hay preguntas planificadas.
     */
    public double completenessRatio() {
        if (totalQuestions == 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) answeredCount / totalQuestions);
    }

    public int remainingBudget() {
        return exchangeBudget == 0 ? Integer.MAX_VALUE : Math.max(0, exchangeBudget - exchangeUsed);
    }
}
