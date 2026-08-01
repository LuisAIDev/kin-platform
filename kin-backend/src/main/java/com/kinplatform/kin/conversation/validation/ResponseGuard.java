package com.kinplatform.kin.conversation.validation;

import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.ResponseValidation;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;

import java.util.ArrayList;
import java.util.List;

/**
 * Guardrail determinista del Conversation Orchestrator (ADR-013).
 *
 * <p>Valida exclusivamente la conformidad de la comunicación del LLM contra la
 * directiva de turno: vacío, longitud, unicidad de pregunta y marcadores
 * prohibidos. Nunca infiere intención ni decisión desde el texto; solo juzga
 * conformidad de comunicación (ADR-013 §4.1, §7.3).</p>
 *
 * <p>Reglas (orden de evaluación): {@code response.empty},
 * {@code response.too_long}, {@code response.multiple_questions} y
 * {@code response.forbidden_marker}; {@code accepted = issues.isEmpty()}.</p>
 */
public final class ResponseGuard {

    private static final String ISSUE_EMPTY = "response.empty";
    private static final String ISSUE_TOO_LONG = "response.too_long";
    private static final String ISSUE_MULTIPLE_QUESTIONS = "response.multiple_questions";
    private static final String ISSUE_FORBIDDEN_MARKER = "response.forbidden_marker";

    /**
     * Valida la respuesta del LLM contra la directiva de turno.
     *
     * @param response  texto producido por el LLM; {@code null} se trata como
     *                  respuesta vacía (issue {@code response.empty})
     * @param directive directiva de turno que enmarca la comunicación
     *                  (obligatoria)
     * @return {@link ResponseValidation} con los issues detectados y
     *         {@code accepted = issues.isEmpty()}
     * @throws IllegalArgumentException si {@code directive} es {@code null}
     */
    public ResponseValidation validate(String response, TurnDirective directive) {
        if (directive == null) {
            throw new IllegalArgumentException("directive no puede ser null");
        }

        List<String> issues = new ArrayList<>();
        if (response == null || response.isBlank()) {
            issues.add(ISSUE_EMPTY);
        }

        TurnConstraints constraints = directive.constraints();
        if (response != null && response.length() > constraints.maxLength()) {
            issues.add(ISSUE_TOO_LONG);
        }
        if (constraints.singleQuestion() && response != null
                && countQuestionMarks(response) > 1) {
            issues.add(ISSUE_MULTIPLE_QUESTIONS);
        }
        if (directive.phase() != ConversationPhase.REPORTING && response != null
                && containsForbiddenMarker(response, constraints.forbiddenMarkers())) {
            issues.add(ISSUE_FORBIDDEN_MARKER);
        }

        return issues.isEmpty()
                ? ResponseValidation.ok()
                : ResponseValidation.rejected(issues);
    }

    private long countQuestionMarks(String response) {
        return response.chars().filter(ch -> ch == '?').count();
    }

    private boolean containsForbiddenMarker(String response, List<String> forbiddenMarkers) {
        for (String marker : forbiddenMarkers) {
            if (response.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
