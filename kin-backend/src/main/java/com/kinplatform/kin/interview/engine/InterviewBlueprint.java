package com.kinplatform.kin.interview.engine;

import com.kinplatform.kin.interview.InterviewQuestion;
import com.kinplatform.kin.interview.InterviewState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plan determinista de la entrevista estratégica (ADR-015, Etapa E3).
 *
 * <p>Contiene la secuencia de {@link InterviewQuestion} organizada por orden y
 * dimensión, con obligatoriedad y follow-ups. Decide en Java qué información
 * falta ({@link #next}) y cuándo la entrevista está completa
 * ({@link #isComplete}), aplicando el presupuesto de intercambios del
 * {@link InterviewState} para evitar el interrogatorio sin fin.</p>
 *
 * <p>Reglas deterministas: el orden es por {@code order} y luego por
 * {@code id}; las preguntas obligatorias nunca se bloquean por un follow-up
 * pendiente (garantiza la completitud alcanzable); un follow-up opcional
 * requiere que su pregunta padre ya esté respondida.</p>
 *
 * <p>Servicio de dominio puro: inmutable una vez construido y sin efectos
 * secundarios.</p>
 */
public class InterviewBlueprint {

    private final List<InterviewQuestion> ordered;
    private final Map<String, InterviewQuestion> byId;
    private final Map<String, String> parentByFollowUpId;

    /**
     * @param questions plan de preguntas; si es {@code null} se usa un plan
     *                  vacío (entrevista completa sin preguntas). Fallas si
     *                  hay identificadores duplicados.
     */
    public InterviewBlueprint(List<InterviewQuestion> questions) {
        List<InterviewQuestion> source = questions == null ? List.of() : questions;
        Map<String, InterviewQuestion> index = new LinkedHashMap<>();
        Map<String, String> parents = new HashMap<>();
        for (InterviewQuestion question : source) {
            if (question == null) {
                continue;
            }
            if (index.containsKey(question.id())) {
                throw new IllegalArgumentException("Identificador duplicado de pregunta: " + question.id());
            }
            index.put(question.id(), question);
            for (String followUpId : question.followUpIds()) {
                parents.putIfAbsent(followUpId, question.id());
            }
        }
        List<InterviewQuestion> sorted = new ArrayList<>(index.values());
        sorted.sort(Comparator.comparingInt(InterviewQuestion::order)
            .thenComparing(InterviewQuestion::id));
        this.ordered = List.copyOf(sorted);
        this.byId = Map.copyOf(index);
        this.parentByFollowUpId = Map.copyOf(parents);
    }

    /**
     * Secuencia ordenada e inmutable de todas las preguntas del plan.
     */
    public List<InterviewQuestion> questions() {
        return ordered;
    }

    /**
     * Número total de preguntas del plan (para el progreso).
     */
    public int totalQuestions() {
        return ordered.size();
    }

    /**
     * Busca una pregunta por su identificador determinista.
     */
    public Optional<InterviewQuestion> question(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Siguiente pregunta a formular: la primera pregunta elegible (no
     * respondida y no bloqueada por un follow-up) en orden determinista.
     * Vacío si la entrevista está completa.
     */
    public Optional<InterviewQuestion> next(InterviewState state) {
        if (isComplete(state)) {
            return Optional.empty();
        }
        for (InterviewQuestion question : ordered) {
            if (isEligible(question, state)) {
                return Optional.of(question);
            }
        }
        return Optional.empty();
    }

    /**
     * Identificadores de las preguntas pendientes (elegibles y no respondidas)
     * en orden determinista; lista vacía si la entrevista está completa.
     */
    public List<String> pendingIds(InterviewState state) {
        if (isComplete(state)) {
            return List.of();
        }
        var pending = new ArrayList<String>();
        for (InterviewQuestion question : ordered) {
            if (isEligible(question, state)) {
                pending.add(question.id());
            }
        }
        return List.copyOf(pending);
    }

    /**
     * La entrevista está completa cuando todas las preguntas obligatorias
     * están respondidas o cuando se agotó el presupuesto de intercambios.
     */
    public boolean isComplete(InterviewState state) {
        if (state == null) {
            return false;
        }
        if (allRequiredAnswered(state)) {
            return true;
        }
        return budgetExhausted(state);
    }

    private boolean isEligible(InterviewQuestion question, InterviewState state) {
        if (state == null || state.hasAnswered(question.id())) {
            return false;
        }
        return !isBlocked(question, state);
    }

    private boolean isBlocked(InterviewQuestion question, InterviewState state) {
        if (question.required()) {
            return false;
        }
        String parent = parentByFollowUpId.get(question.id());
        return parent != null && !state.hasAnswered(parent);
    }

    private boolean allRequiredAnswered(InterviewState state) {
        for (InterviewQuestion question : ordered) {
            if (question.required() && !state.hasAnswered(question.id())) {
                return false;
            }
        }
        return true;
    }

    private boolean budgetExhausted(InterviewState state) {
        return state.exchangeBudget() > 0 && state.exchangeUsed() >= state.exchangeBudget();
    }
}
