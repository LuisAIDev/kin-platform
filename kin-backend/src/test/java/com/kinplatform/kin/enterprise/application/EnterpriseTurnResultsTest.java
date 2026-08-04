package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseTurnResultsTest {

    @Test
    void constructorConResultadosNulos_normalizaAEmpy() {
        var results = new EnterpriseTurnResults(null, null, null, null, null);

        assertTrue(results.recommendations().isEmpty());
        assertTrue(results.opportunities().isEmpty());
        assertTrue(results.knowledge().isEmpty());
        assertTrue(results.riskResult().isEmpty());
    }

    @Test
    void constructorConservaLosResultadosReales() {
        var projectId = UUID.randomUUID();
        var recommendations = EngineTestFixtures.recommendations(0.8);
        var opportunities = EngineTestFixtures.opportunities(0.8);
        var knowledge = EngineTestFixtures.knowledge(0.8);
        var risks = EngineTestFixtures.riskResult(0.8);

        var results = new EnterpriseTurnResults(projectId, recommendations, opportunities, knowledge, risks);

        assertEquals(projectId, results.projectId());
        assertEquals(recommendations, results.recommendations());
        assertEquals(opportunities, results.opportunities());
        assertEquals(knowledge, results.knowledge());
        assertEquals(risks, results.riskResult());
    }

    @Test
    void empty_devuelveTodosLosResultadosVacios() {
        var results = EnterpriseTurnResults.empty();

        assertEquals(null, results.projectId());
        assertTrue(results.recommendations().isEmpty());
        assertTrue(results.opportunities().isEmpty());
        assertTrue(results.knowledge().isEmpty());
        assertTrue(results.riskResult().isEmpty());
    }

    @Test
    void resultadosRealesNoVacios() {
        var results = new EnterpriseTurnResults(
            UUID.randomUUID(),
            EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8),
            EngineTestFixtures.riskResult(0.8));

        assertFalse(results.recommendations().isEmpty());
        assertFalse(results.opportunities().isEmpty());
        assertFalse(results.knowledge().isEmpty());
        assertFalse(results.riskResult().isEmpty());
    }
}
