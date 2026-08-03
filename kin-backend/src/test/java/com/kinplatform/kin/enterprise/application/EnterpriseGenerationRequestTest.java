package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseGenerationRequestTest {

    @Test
    void requestValido_exponerValores() {
        var context = EngineTestFixtures.contextWithAll();
        var recommendations = EngineTestFixtures.recommendations(0.8);
        var opportunities = EngineTestFixtures.opportunities(0.8);
        var knowledge = EngineTestFixtures.knowledge(0.8);
        var risk = EngineTestFixtures.riskResult(0.8);
        var projectId = UUID.randomUUID();

        var request = new EnterpriseGenerationRequest(
            projectId, context, recommendations, opportunities, knowledge, risk);

        assertEquals(projectId, request.projectId());
        assertSame(context, request.context());
        assertSame(recommendations, request.recommendations());
        assertSame(opportunities, request.opportunities());
        assertSame(knowledge, request.knowledge());
        assertSame(risk, request.riskResult());
    }

    @Test
    void projectIdNulo_lanza() {
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseGenerationRequest(
            null, EngineTestFixtures.contextWithAll(), null, null, null, null));
    }

    @Test
    void contextNulo_lanza() {
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseGenerationRequest(
            UUID.randomUUID(), null, null, null, null, null));
    }

    @Test
    void pipelineNulos_usanPatronEmpty() {
        var request = new EnterpriseGenerationRequest(
            UUID.randomUUID(), EngineTestFixtures.contextWithAll(), null, null, null, null);

        assertNotNull(request.recommendations());
        assertNotNull(request.opportunities());
        assertNotNull(request.knowledge());
        assertNotNull(request.riskResult());
        assertTrue(request.recommendations().isEmpty());
        assertTrue(request.opportunities().isEmpty());
        assertTrue(request.knowledge().isEmpty());
        assertTrue(request.riskResult().isEmpty());
    }

    @Test
    void contextCompleto_usaCoberturaParaLosMotores() {
        var context = EngineTestFixtures.contextWithAll();
        assertTrue(context.hasKnownDimensions());
        assertTrue(context.coverageRatio() > 0.0);
    }
}
