package com.kinplatform.kin.interview.engine;

import com.kinplatform.kin.interview.AnswerRules;
import com.kinplatform.kin.interview.AnswerValidation;

import java.util.List;
import java.util.Locale;

/**
 * Validador determinista de respuestas de la entrevista (ADR-015, Etapa E3).
 *
 * <p>Aplica en Java las reglas en orden: vacío, longitud mínima, formato
 * (expresión regular configurable) y palabras clave. Si la respuesta no es
 * aceptable y las reglas lo permiten, devuelve un refinamiento acotado por
 * {@code maxRefinements}; en caso contrario, la rechaza con su motivo. Nunca
 * consulta al LLM.</p>
 *
 * <p>Servicio de dominio puro: stateless y reentrante (el conteo de
 * refinamientos viaja en el {@link AnswerValidation}, no en el estado del
 * servicio).</p>
 */
public class AnswerValidator {

    private static final String REASON_EMPTY = "Respuesta vacía";
    private static final String REASON_MIN_LENGTH = "Respuesta demasiado breve";
    private static final String REASON_FORMAT = "Formato no válido";
    private static final String REASON_KEYWORDS = "Faltan palabras clave";

    /**
     * Valida una respuesta sin refinamientos previos (equivalente a
     * {@code validate(answer, rules, 0)}).
     */
    public AnswerValidation validate(String answer, AnswerRules rules) {
        return validate(answer, rules, 0);
    }

    /**
     * Valida una respuesta considerando los refinamientos ya aplicados a la
     * pregunta. Devuelve una aceptación si cumple todas las reglas; un
     * refinamiento si falla alguna y {@code allowRefinement} con presupuesto
     * disponible; un rechazo en caso contrario.
     *
     * @param answer             texto de la respuesta (se trima antes de medir)
     * @param rules              reglas aplicables; si es {@code null} se usan
     *                           {@link AnswerRules#defaults()}
     * @param currentRefinements refinamientos ya consumidos para la pregunta
     */
    public AnswerValidation validate(String answer, AnswerRules rules, int currentRefinements) {
        AnswerRules effective = rules == null ? AnswerRules.defaults() : rules;
        if (answer == null || answer.isBlank()) {
            return decide(REASON_EMPTY, effective, currentRefinements);
        }
        String trimmed = answer.trim();
        if (trimmed.length() < effective.minLength()) {
            return decide(REASON_MIN_LENGTH + " (mínimo " + effective.minLength() + " caracteres)",
                effective, currentRefinements);
        }
        if (effective.hasFormatRequirement() && !matchesFormat(trimmed, effective.requiredFormat())) {
            return decide(REASON_FORMAT + " (se esperaba: " + effective.requiredFormat() + ")",
                effective, currentRefinements);
        }
        if (effective.hasKeywordRequirements() && !containsKeywords(trimmed, effective.minKeywords())) {
            return decide(REASON_KEYWORDS + ": " + effective.minKeywords(), effective, currentRefinements);
        }
        return AnswerValidation.valid();
    }

    private AnswerValidation decide(String reason, AnswerRules rules, int currentRefinements) {
        if (rules.allowRefinement() && currentRefinements < rules.maxRefinements()) {
            return AnswerValidation.refinement(reason, currentRefinements + 1);
        }
        return AnswerValidation.rejected(reason);
    }

    private boolean containsKeywords(String answer, List<String> keywords) {
        String normalized = answer.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()
                || !normalized.contains(keyword.trim().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesFormat(String answer, String format) {
        try {
            return answer.matches(format);
        } catch (RuntimeException ex) {
            return true;
        }
    }
}
