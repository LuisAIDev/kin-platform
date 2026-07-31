package com.kinplatform.kin.reporting.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportModelTest {

    @Test
    void defaultModel_deberiaTenerValoresEsperados() {
        var model = ReportModel.defaultModel();
        assertEquals("v1", model.version());
        assertEquals("2.0.0-alpha.1", model.architectureVersion());
        assertEquals(5, model.nextStepsLimit());
        assertNotNull(model.description());
    }

    @Test
    void modelo_deberiaAceptarNulos() {
        var model = new ReportModel(null, null, null, -3);
        assertEquals("", model.version());
        assertEquals("", model.architectureVersion());
        assertEquals("", model.description());
        assertEquals(0, model.nextStepsLimit());
    }
}
