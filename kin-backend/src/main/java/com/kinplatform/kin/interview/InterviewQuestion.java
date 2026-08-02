package com.kinplatform.kin.interview;

import com.kinplatform.kin.context.AnalyzedDimension;

import java.util.List;

/**
 * Pregunta de la entrevista estratégica (ADR-015).
 *
 * <p>Representa un ítem del {@code InterviewBlueprint} (Etapa E3): una pregunta
 * determinista dirigida a una {@link AnalyzedDimension}, con un {@code topic}
 * semántico que el LLM únicamente formulará en lenguaje natural, las reglas de
 * validación de su respuesta y los identificadores de sus follow-ups (preguntas
 * de profundización adaptadas al tipo de respuesta). Inmutable y defensivo.</p>
 */
public record InterviewQuestion(
    String id,
    AnalyzedDimension dimension,
    String topic,
    boolean required,
    int order,
    AnswerRules rules,
    List<String> followUpIds
) {

    public InterviewQuestion {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id no puede ser null o vacío");
        }
        if (dimension == null) {
            throw new IllegalArgumentException("dimension no puede ser null");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic no puede ser null o vacío");
        }
        order = Math.max(0, order);
        rules = rules == null ? AnswerRules.defaults() : rules;
        followUpIds = followUpIds == null ? List.of() : List.copyOf(followUpIds);
    }

    /**
     * Pregunta obligatoria con reglas por defecto.
     */
    public static InterviewQuestion required(String id, AnalyzedDimension dimension, String topic, int order) {
        return new InterviewQuestion(id, dimension, topic, true, order, AnswerRules.defaults(), List.of());
    }

    /**
     * Pregunta opcional con reglas por defecto.
     */
    public static InterviewQuestion optional(String id, AnalyzedDimension dimension, String topic, int order) {
        return new InterviewQuestion(id, dimension, topic, false, order, AnswerRules.defaults(), List.of());
    }

    public boolean hasFollowUps() {
        return !followUpIds.isEmpty();
    }
}
