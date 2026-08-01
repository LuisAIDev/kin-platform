package com.kinplatform.kin.conversation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnConstraintsTest {

    @Test
    void question_deberiaConfigurarModoPregunta() {
        var constraints = TurnConstraints.question();
        assertEquals(280, constraints.maxLength());
        assertTrue(constraints.singleQuestion());
        assertTrue(constraints.forbiddenMarkers().contains("=== CONSULTING REPORT ==="));
        assertTrue(constraints.forbiddenMarkers().contains("## INFORME DE VIABILIDAD"));
        assertTrue(constraints.forbiddenMarkers().contains("Scoring:"));
    }

    @Test
    void reportExplanation_deberiaConfigurarModoReporte() {
        var constraints = TurnConstraints.reportExplanation();
        assertEquals(1200, constraints.maxLength());
        assertFalse(constraints.singleQuestion());
        assertTrue(constraints.forbiddenMarkers().isEmpty());
    }

    @Test
    void constructor_deberiaProtegerListaDeMarcadores() {
        var markers = new ArrayList<>(List.of("M1"));
        var constraints = new TurnConstraints(100, true, markers);

        markers.add("M2");
        assertThrows(UnsupportedOperationException.class,
            () -> constraints.forbiddenMarkers().add("M3"));
        assertEquals(1, constraints.forbiddenMarkers().size());
    }

    @Test
    void constructor_deberiaAceptarMarcadoresNulos() {
        var constraints = new TurnConstraints(100, true, null);
        assertTrue(constraints.forbiddenMarkers().isEmpty());
    }

    @Test
    void constructor_deberiaRechazarMaxLengthNoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new TurnConstraints(0, true, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TurnConstraints(-5, false, List.of()));
    }
}
