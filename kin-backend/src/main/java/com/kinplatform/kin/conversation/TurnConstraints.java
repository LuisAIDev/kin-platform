package com.kinplatform.kin.conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Restricciones de comunicación de un turno: límite de longitud, unicidad de
 * pregunta y marcadores prohibidos. Es una decisión 100 % Java (ADR-013): el
 * guardrail evalúa la respuesta del LLM contra estas restricciones, nunca
 * parsea decisiones desde texto.
 */
public record TurnConstraints(
    int maxLength,
    boolean singleQuestion,
    List<String> forbiddenMarkers
) {

    /**
     * Tope de longitud para una respuesta en modo QUESTION. Es un presupuesto
     * suave: si el LLM produce contenido útil por encima del tope, la respuesta
     * se entrega igualmente (política de longitud suave, {@code ResponseGuard.requiresFallback}).
     * El valor refleja respuestas consultivas reales (listas, viñetas y
     * párrafos), no solo una pregunta de entrevista.
     */
    public static final int QUESTION_MAX_LENGTH = 4000;

    /**
     * Tope de longitud para la explicación del {@code ConsultingReport}. Igual
     * que QUESTION, es un presupuesto suave: el contenido útil nunca se descarta.
     */
    public static final int REPORT_EXPLANATION_MAX_LENGTH = 8000;

    private static final List<String> REPORT_MARKERS = List.of(
        "=== CONSULTING REPORT ===",
        "## INFORME DE VIABILIDAD",
        "Scoring:"
    );

    public TurnConstraints {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength debe ser positivo");
        }
        forbiddenMarkers = (forbiddenMarkers != null)
                ? Collections.unmodifiableList(new ArrayList<>(forbiddenMarkers))
                : List.of();
    }

    /**
     * Restricciones del modo QUESTION: una sola pregunta, acotada, sin
     * marcadores de reporte (la respuesta no debe revelar análisis de reporte
     * en fase de exploración).
     */
    public static TurnConstraints question() {
        return new TurnConstraints(QUESTION_MAX_LENGTH, true, REPORT_MARKERS);
    }

    /**
     * Restricciones del modo EXPLAIN_REPORT: explicación extensa del
     * {@code ConsultingReport}, sin restricción de pregunta única ni de
     * marcadores (el propio reporte es la fuente).
     */
    public static TurnConstraints reportExplanation() {
        return new TurnConstraints(REPORT_EXPLANATION_MAX_LENGTH, false, List.of());
    }
}
