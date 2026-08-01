package com.kinplatform.kin.conversation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseValidationTest {

    @Test
    void ok_deberiaEstarAceptadaSinIssues() {
        var validation = ResponseValidation.ok();
        assertTrue(validation.accepted());
        assertTrue(validation.issues().isEmpty());
    }

    @Test
    void rejected_deberiaEstarRechazadaConIssues() {
        var validation = ResponseValidation.rejected(List.of("response.empty"));
        assertFalse(validation.accepted());
        assertEquals(List.of("response.empty"), validation.issues());
    }

    @Test
    void rejected_deberiaAceptarIssuesNulos() {
        var validation = ResponseValidation.rejected(null);
        assertFalse(validation.accepted());
        assertTrue(validation.issues().isEmpty());
    }

    @Test
    void validation_deberiaProtegerListaDeIssues() {
        var issues = new ArrayList<>(List.of("response.empty"));
        var validation = new ResponseValidation(false, issues);

        issues.add("response.too_long");
        assertThrows(UnsupportedOperationException.class,
            () -> validation.issues().add("otro"));
        assertEquals(1, validation.issues().size());
    }

    @Test
    void validation_deberiaAceptarIssuesNulosEnConstructor() {
        var validation = new ResponseValidation(true, null);
        assertTrue(validation.accepted());
        assertTrue(validation.issues().isEmpty());
    }
}
