package com.kinplatform.kin.conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado de validar la comunicación del LLM contra la directiva de turno.
 * {@code accepted} es cierto solo si no hay issues: Java decide la conformidad
 * de la comunicación, nunca la intención (ADR-013).
 */
public record ResponseValidation(
    boolean accepted,
    List<String> issues
) {

    public ResponseValidation {
        issues = (issues != null)
                ? Collections.unmodifiableList(new ArrayList<>(issues))
                : List.of();
    }

    public static ResponseValidation ok() {
        return new ResponseValidation(true, List.of());
    }

    public static ResponseValidation rejected(List<String> issues) {
        return new ResponseValidation(false, issues);
    }
}
