package com.kinplatform.kin.knowledge.orchestrator;

import java.util.Map;
import java.util.Set;

/**
 * Estados de la máquina de estados del Knowledge Orchestrator (especificación
 * Fase 5). Cada estado tiene responsabilidad única y las transiciones son
 * deterministas (matriz {@link #canTransition}).
 */
public enum OrchestrationState {

    IDLE("Espera de solicitud"),
    PLANNING("Planificación"),
    CACHE_LOOKUP("Consulta de caché"),
    PROVIDER_SELECTION("Selección de proveedores"),
    FETCHING("Adquisición coordinada"),
    VALIDATION("Validación"),
    RANKING("Ranking"),
    ASSEMBLING("Ensamblado"),
    COMPLETED("Completado"),
    FAILED("Fallido");

    private static final Map<OrchestrationState, Set<OrchestrationState>> TRANSITIONS = Map.ofEntries(
        Map.entry(IDLE, Set.of(PLANNING, FAILED)),
        Map.entry(PLANNING, Set.of(CACHE_LOOKUP, COMPLETED, FAILED)),
        Map.entry(CACHE_LOOKUP, Set.of(PROVIDER_SELECTION, COMPLETED, FAILED)),
        Map.entry(PROVIDER_SELECTION, Set.of(FETCHING, COMPLETED, FAILED)),
        Map.entry(FETCHING, Set.of(VALIDATION, COMPLETED, FAILED)),
        Map.entry(VALIDATION, Set.of(RANKING, FAILED)),
        Map.entry(RANKING, Set.of(ASSEMBLING, FAILED)),
        Map.entry(ASSEMBLING, Set.of(COMPLETED, FAILED)),
        Map.entry(COMPLETED, Set.of()),
        Map.entry(FAILED, Set.of()));

    private final String displayName;

    OrchestrationState(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * Valida determinísticamente si la transición {@code from → to} está
     * permitida por la máquina de estados.
     */
    public static boolean canTransition(OrchestrationState from, OrchestrationState to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
