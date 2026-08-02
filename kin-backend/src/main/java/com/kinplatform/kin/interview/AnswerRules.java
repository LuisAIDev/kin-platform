package com.kinplatform.kin.interview;

import java.util.List;

/**
 * Reglas deterministas de validación de una respuesta de entrevista (ADR-015).
 *
 * <p>Define los umbrales que {@code AnswerValidator} (Etapa E3) aplicará en Java
 * para aceptar, rechazar o pedir refinamiento de la respuesta del usuario.
 * Inmutable y defensivo: {@code minKeywords} se copia y {@code minLength} /
 * {@code maxRefinements} se acotan a valores no negativos.</p>
 */
public record AnswerRules(
    int minLength,
    List<String> minKeywords,
    String requiredFormat,
    boolean allowRefinement,
    int maxRefinements
) {

    public AnswerRules {
        minLength = Math.max(0, minLength);
        minKeywords = minKeywords == null ? List.of() : List.copyOf(minKeywords);
        requiredFormat = requiredFormat == null ? "" : requiredFormat;
        maxRefinements = Math.max(0, maxRefinements);
    }

    /**
     * Reglas por defecto: sin longitud mínima, sin palabras clave, sin formato
     * requerido, refinamiento permitido (máximo 1).
     */
    public static AnswerRules defaults() {
        return new AnswerRules(0, List.of(), "", true, 1);
    }

    /**
     * Reglas con exigencia de longitud mínima y presupuesto de refinamiento.
     */
    public static AnswerRules of(int minLength, boolean allowRefinement, int maxRefinements) {
        return new AnswerRules(minLength, List.of(), "", allowRefinement, maxRefinements);
    }

    public boolean hasKeywordRequirements() {
        return !minKeywords.isEmpty();
    }

    public boolean hasFormatRequirement() {
        return !requiredFormat.isBlank();
    }
}
