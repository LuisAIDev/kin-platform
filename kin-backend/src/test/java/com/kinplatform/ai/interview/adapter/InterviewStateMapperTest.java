package com.kinplatform.ai.interview.adapter;

import com.kinplatform.kin.interview.InterviewAnswer;
import com.kinplatform.kin.interview.InterviewState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewStateMapperTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final int BUDGET = 20;

    private final InterviewStateMapper mapper = new InterviewStateMapper();

    @Test
    void toDomain_deberiaRestaurarElEstadoCompleto() {
        var original = InterviewState.restore(
            PROJECT_ID,
            Map.of("q-proyecto", InterviewAnswer.of("q-proyecto", "Mi App")),
            List.of("q-sector", "q-cliente"),
            "q-sector",
            Map.of("q-proyecto", 1),
            false,
            BUDGET,
            2
        );

        var restored = mapper.toDomain(PROJECT_ID, mapper.toData(original));

        assertEquals(PROJECT_ID, restored.projectId());
        assertEquals(original.answered(), restored.answered());
        assertEquals(original.pending(), restored.pending());
        assertEquals("q-sector", restored.current());
        assertEquals(original.refinements(), restored.refinements());
        assertEquals(false, restored.complete());
        assertEquals(BUDGET, restored.exchangeBudget());
        assertEquals(2, restored.exchangeUsed());
    }

    @Test
    void toData_conEstadoVacio_deberiaConservarColeccionesVacias() {
        var empty = InterviewState.empty(PROJECT_ID);

        var data = mapper.toData(empty);

        assertTrue(data.answered().isEmpty());
        assertTrue(data.pending().isEmpty());
        assertTrue(data.refinements().isEmpty());
        assertNull(data.current());
        assertEquals(false, data.complete());
        assertEquals(BUDGET, data.exchangeBudget());
        assertEquals(0, data.exchangeUsed());
    }

    @Test
    void toDomain_conDataVacia_deberiaRestaurarEstadoVacio() {
        var restored = mapper.toDomain(PROJECT_ID, mapper.toData(InterviewState.empty(PROJECT_ID)));

        assertTrue(restored.answered().isEmpty());
        assertTrue(restored.pending().isEmpty());
        assertTrue(restored.refinements().isEmpty());
        assertNull(restored.current());
        assertEquals(BUDGET, restored.exchangeBudget());
    }

    @Test
    void toDomain_deberiaMantenerCurrentNulo() {
        var restored = mapper.toDomain(PROJECT_ID, new InterviewStateData(
            Map.of(), List.of(), null, Map.of(), false, BUDGET, 0));

        assertNull(restored.current());
    }

    @Test
    void toDomain_conDataNull_deberiaDevolverNull() {
        assertNull(mapper.toDomain(PROJECT_ID, null));
    }

    @Test
    void toDomain_conProjectIdNull_deberiaDevolverNull() {
        assertNull(mapper.toDomain(null, mapper.toData(InterviewState.empty(PROJECT_ID))));
    }

    @Test
    void toData_conStateNull_deberiaDevolverNull() {
        assertNull(mapper.toData(null));
    }

    @Test
    void toDomain_conMapasNulos_deberiaNormalizarlosAVacios() {
        var restored = mapper.toDomain(PROJECT_ID, new InterviewStateData(
            null, List.of(), "q-proyecto", null, false, BUDGET, 0));

        assertTrue(restored.answered().isEmpty());
        assertTrue(restored.refinements().isEmpty());
        assertEquals("q-proyecto", restored.current());
    }
}
