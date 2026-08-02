package com.kinplatform.ai.interview.adapter;

import com.kinplatform.kin.interview.InterviewAnswer;

import java.util.List;
import java.util.Map;

/**
 * DTO propio del adaptador con el formato de persistencia del estado de la
 * entrevista (ADR-015).
 *
 * <p>El {@code projectId} es la clave primaria de {@link InterviewStateEntity}
 * y no se duplica en el payload JSON; el resto del estado se serializa como
 * texto. Mantener este DTO separado del objeto de dominio evita acoplar el
 * formato de persistencia a {@code kin.interview}.</p>
 */
public record InterviewStateData(
    Map<String, InterviewAnswer> answered,
    List<String> pending,
    String current,
    Map<String, Integer> refinements,
    boolean complete,
    int exchangeBudget,
    int exchangeUsed
) {
}
