package com.kinplatform.kin.interview;

/**
 * Resultado determinista de la validación de una respuesta (ADR-015).
 *
 * <p>Producida por {@code AnswerValidator} (Etapa E3) en Java: {@code accepted}
 * indica si la respuesta habilita continuar; {@code requiresRefinement} indica
 * que se debe pedir más detalle (si {@code allowRefinement} y el presupuesto lo
 * permiten). {@code refinementCount} registra cuántas veces se ha refinado la
 * pregunta para acotar el interrogatorio.</p>
 */
public record AnswerValidation(
    boolean accepted,
    String reason,
    boolean requiresRefinement,
    int refinementCount
) {

    public AnswerValidation {
        reason = reason == null ? "" : reason;
        refinementCount = Math.max(0, refinementCount);
    }

    public static AnswerValidation valid() {
        return new AnswerValidation(true, "", false, 0);
    }

    public static AnswerValidation rejected(String reason) {
        return new AnswerValidation(false, reason == null ? "" : reason, false, 0);
    }

    public static AnswerValidation refinement(String reason, int refinementCount) {
        return new AnswerValidation(false, reason == null ? "" : reason, true, refinementCount);
    }

    public boolean isAccepted() {
        return accepted;
    }
}
