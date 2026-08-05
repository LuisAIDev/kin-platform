package com.kinplatform.kin.knowledge.orchestrator;

import org.junit.jupiter.api.Test;

import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.ASSEMBLING;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.CACHE_LOOKUP;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.COMPLETED;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.FAILED;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.FETCHING;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.IDLE;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.PLANNING;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.PROVIDER_SELECTION;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.RANKING;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationState.VALIDATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrchestrationStateTest {

    @Test
    void transicionesValidas_deberianCumplirse() {
        assertTrue(OrchestrationState.canTransition(IDLE, PLANNING));
        assertTrue(OrchestrationState.canTransition(IDLE, FAILED));
        assertTrue(OrchestrationState.canTransition(PLANNING, CACHE_LOOKUP));
        assertTrue(OrchestrationState.canTransition(PLANNING, COMPLETED));
        assertTrue(OrchestrationState.canTransition(PLANNING, FAILED));
        assertTrue(OrchestrationState.canTransition(CACHE_LOOKUP, PROVIDER_SELECTION));
        assertTrue(OrchestrationState.canTransition(CACHE_LOOKUP, COMPLETED));
        assertTrue(OrchestrationState.canTransition(PROVIDER_SELECTION, FETCHING));
        assertTrue(OrchestrationState.canTransition(PROVIDER_SELECTION, COMPLETED));
        assertTrue(OrchestrationState.canTransition(FETCHING, VALIDATION));
        assertTrue(OrchestrationState.canTransition(FETCHING, COMPLETED));
        assertTrue(OrchestrationState.canTransition(VALIDATION, RANKING));
        assertTrue(OrchestrationState.canTransition(RANKING, ASSEMBLING));
        assertTrue(OrchestrationState.canTransition(ASSEMBLING, COMPLETED));
    }

    @Test
    void transicionesInvalidas_deberianRechazarse() {
        assertFalse(OrchestrationState.canTransition(IDLE, COMPLETED));
        assertFalse(OrchestrationState.canTransition(PLANNING, IDLE));
        assertFalse(OrchestrationState.canTransition(COMPLETED, FAILED));
        assertFalse(OrchestrationState.canTransition(FAILED, PLANNING));
        assertFalse(OrchestrationState.canTransition(VALIDATION, COMPLETED));
        assertFalse(OrchestrationState.canTransition(null, PLANNING));
        assertFalse(OrchestrationState.canTransition(IDLE, null));
    }

    @Test
    void estadosTerminales_deberianDetenerse() {
        assertTrue(COMPLETED.isTerminal());
        assertTrue(FAILED.isTerminal());
        assertFalse(IDLE.isTerminal());
        assertFalse(PLANNING.isTerminal());
    }

    @Test
    void displayNames_deberianExponerse() {
        assertEquals("Planificación", PLANNING.displayName());
        assertEquals("Completado", COMPLETED.displayName());
        assertEquals("Fallido", FAILED.displayName());
        assertEquals("Adquisición coordinada", FETCHING.displayName());
    }
}
