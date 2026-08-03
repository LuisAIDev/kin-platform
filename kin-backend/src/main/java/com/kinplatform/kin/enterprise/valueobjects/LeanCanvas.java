package com.kinplatform.kin.enterprise.valueobjects;

import java.util.List;

/**
 * Modelo de negocio Lean Canvas (value object).
 *
 * <p>Contiene los nueve bloques clásicos del Lean Canvas: problema, segmentos
 * de cliente, propuesta única de valor, solución, canales, flujos de ingreso,
 * estructura de costes, métricas clave y ventaja injusta. Cada bloque es una
 * lista inmutable de strings no vacíos; el objeto completo es inmutable.</p>
 *
 * <p>Producido por {@code BusinessModelEngine} y consumido por el ensamblador
 * del documento Lean Canvas.</p>
 */
public record LeanCanvas(
    List<String> problem,
    List<String> customerSegments,
    List<String> uniqueValueProposition,
    List<String> solution,
    List<String> channels,
    List<String> revenueStreams,
    List<String> costStructure,
    List<String> keyMetrics,
    List<String> unfairAdvantage
) {

    public LeanCanvas {
        problem = ValueObjects.immutableNotBlank(problem, "problem");
        customerSegments = ValueObjects.immutableNotBlank(customerSegments, "customerSegments");
        uniqueValueProposition = ValueObjects.immutableNotBlank(uniqueValueProposition, "uniqueValueProposition");
        solution = ValueObjects.immutableNotBlank(solution, "solution");
        channels = ValueObjects.immutableNotBlank(channels, "channels");
        revenueStreams = ValueObjects.immutableNotBlank(revenueStreams, "revenueStreams");
        costStructure = ValueObjects.immutableNotBlank(costStructure, "costStructure");
        keyMetrics = ValueObjects.immutableNotBlank(keyMetrics, "keyMetrics");
        unfairAdvantage = ValueObjects.immutableNotBlank(unfairAdvantage, "unfairAdvantage");
    }

    /**
     * Crea un Lean Canvas vacío: los nueve bloques quedan vacíos.
     */
    public static LeanCanvas empty() {
        return new LeanCanvas(List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Crea un Lean Canvas a partir de los nueve bloques clásicos.
     */
    public static LeanCanvas of(List<String> problem, List<String> customerSegments,
                                List<String> uniqueValueProposition, List<String> solution,
                                List<String> channels, List<String> revenueStreams,
                                List<String> costStructure, List<String> keyMetrics,
                                List<String> unfairAdvantage) {
        return new LeanCanvas(problem, customerSegments, uniqueValueProposition, solution,
            channels, revenueStreams, costStructure, keyMetrics, unfairAdvantage);
    }
}
