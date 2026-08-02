package com.kinplatform.kin.interview;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Estado inmutable del progreso de la entrevista por proyecto (ADR-015).
 *
 * <p>Registra qué preguntas se respondieron ({@code answered}), cuáles quedan
 * pendientes ({@code pending}), la pregunta actual ({@code current}), el número
 * de refinamientos por pregunta ({@code refinements}), la completitud
 * ({@code complete}) y el presupuesto de intercambios ({@code exchangeBudget} /
 * {@code exchangeUsed}) para evitar el interrogatorio sin fin. Todos los
 * mutadores devuelven una copia nueva (inmutabilidad).</p>
 */
public record InterviewState(
    UUID projectId,
    Map<String, InterviewAnswer> answered,
    List<String> pending,
    String current,
    Map<String, Integer> refinements,
    boolean complete,
    int exchangeBudget,
    int exchangeUsed
) {

    public static final int DEFAULT_EXCHANGE_BUDGET = 20;

    public InterviewState {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId no puede ser null");
        }
        answered = answered == null ? Map.of() : Map.copyOf(answered);
        pending = pending == null ? List.of() : List.copyOf(pending);
        current = current == null || current.isBlank() ? null : current;
        refinements = refinements == null ? Map.of() : Map.copyOf(refinements);
        exchangeBudget = Math.max(0, exchangeBudget);
        exchangeUsed = Math.max(0, exchangeUsed);
    }

    /**
     * Estado vacío con presupuesto por defecto ({@value DEFAULT_EXCHANGE_BUDGET}).
     */
    public static InterviewState empty(UUID projectId) {
        return new InterviewState(projectId, Map.of(), List.of(), null, Map.of(), false, DEFAULT_EXCHANGE_BUDGET, 0);
    }

    /**
     * Estado vacío con presupuesto de intercambios explícito (0 = sin límite).
     */
    public static InterviewState empty(UUID projectId, int exchangeBudget) {
        return new InterviewState(projectId, Map.of(), List.of(), null, Map.of(), false, exchangeBudget, 0);
    }

    /**
     * Restaura un estado persistido sin pasar por las factories de creación.
     */
    public static InterviewState restore(UUID projectId, Map<String, InterviewAnswer> answered, List<String> pending,
                                         String current, Map<String, Integer> refinements, boolean complete,
                                         int exchangeBudget, int exchangeUsed) {
        return new InterviewState(projectId, answered, pending, current, refinements, complete, exchangeBudget, exchangeUsed);
    }

    public InterviewState withAnswered(Map<String, InterviewAnswer> answered) {
        return new InterviewState(projectId, answered, pending, current, refinements, complete, exchangeBudget, exchangeUsed);
    }

    public InterviewState withPending(List<String> pending) {
        return new InterviewState(projectId, answered, pending, current, refinements, complete, exchangeBudget, exchangeUsed);
    }

    public InterviewState withCurrent(String current) {
        return new InterviewState(projectId, answered, pending, current, refinements, complete, exchangeBudget, exchangeUsed);
    }

    public InterviewState withRefinements(Map<String, Integer> refinements) {
        return new InterviewState(projectId, answered, pending, current, refinements, complete, exchangeBudget, exchangeUsed);
    }

    public InterviewState withComplete(boolean complete) {
        return new InterviewState(projectId, answered, pending, current, refinements, complete, exchangeBudget, exchangeUsed);
    }

    public InterviewState withExchangeUsed(int exchangeUsed) {
        return new InterviewState(projectId, answered, pending, current, refinements, complete, exchangeBudget, exchangeUsed);
    }

    public int answeredCount() {
        return answered.size();
    }

    public int pendingCount() {
        return pending.size();
    }

    public boolean hasAnswered(String questionId) {
        return answered.containsKey(questionId);
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean hasPendingQuestions() {
        return !pending.isEmpty();
    }

    public Optional<String> currentQuestionId() {
        return Optional.ofNullable(current);
    }

    /**
     * Intercambios restantes hasta el presupuesto; {@link Integer#MAX_VALUE} si
     * el presupuesto es ilimitado (0).
     */
    public int remainingBudget() {
        return exchangeBudget == 0 ? Integer.MAX_VALUE : Math.max(0, exchangeBudget - exchangeUsed);
    }

    public InterviewProgress toProgress(int totalQuestions) {
        return InterviewProgress.of(answered.size(), pending.size(), totalQuestions, exchangeUsed, exchangeBudget, complete);
    }
}
