package com.kinplatform.kin.interview;

import com.kinplatform.kin.context.AnalyzedDimension;

/**
 * Directiva de la pregunta de entrevista que el LLM debe formular (ADR-015).
 *
 * <p>Es la pieza que viaja hacia la capa de prompt: contiene el identificador de
 * la pregunta, la dimensión objetivo, el {@code topic} semántico (que el LLM
 * redacta en lenguaje natural) y las reglas de validación. El LLM nunca decide
 * la pregunta; solo la formula.</p>
 */
public record InterviewDirective(
    String questionId,
    AnalyzedDimension dimension,
    String topic,
    AnswerRules rules
) {

    public InterviewDirective {
        if (questionId == null || questionId.isBlank()) {
            throw new IllegalArgumentException("questionId no puede ser null o vacío");
        }
        if (dimension == null) {
            throw new IllegalArgumentException("dimension no puede ser null");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic no puede ser null o vacío");
        }
        rules = rules == null ? AnswerRules.defaults() : rules;
    }

    public static InterviewDirective of(String questionId, AnalyzedDimension dimension,
                                        String topic, AnswerRules rules) {
        return new InterviewDirective(questionId, dimension, topic, rules);
    }
}
