package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryPolicyTest {

    private final QueryPolicyConfig config = QueryPolicyConfig.defaults();

    @Test
    void stableTopic_temaEstable_deberiaSoloModelo() {
        var rule = new StableTopicQueryRule();

        var result = rule.evaluate(KnowledgeRequest.of("Scrum", List.of("scrum")), config);

        assertTrue(result.modelOnly());
        assertFalse(result.consultExternal());
    }

    @Test
    void stableTopic_sinTemasConfigurados_deberiaExterno() {
        var rule = new StableTopicQueryRule();

        var result = rule.evaluate(KnowledgeRequest.of("scrum", List.of()), QueryPolicyConfig.dev());

        assertTrue(result.consultExternal());
    }

    @Test
    void stableTopic_temaNoEstable_deberiaExterno() {
        var rule = new StableTopicQueryRule();

        var result = rule.evaluate(KnowledgeRequest.of("panadería", List.of("pan")), config);

        assertTrue(result.consultExternal());
        assertEquals(QueryMode.EXTERNAL, result.mode());
    }

    @Test
    void cacheFirst_dentroDelTtl_deberiaSoloCache() {
        var rule = new CacheFirstQueryRule();
        var request = new KnowledgeRequest("tema", Set.of(), List.of(), 5, Duration.ofHours(12));

        var result = rule.evaluate(request, config);

        assertTrue(result.cacheOnly());
    }

    @Test
    void cacheFirst_fueraDelTtl_deberiaExterno() {
        var rule = new CacheFirstQueryRule();
        var request = new KnowledgeRequest("tema", Set.of(), List.of(), 5, Duration.ofDays(30));

        var result = rule.evaluate(request, config);

        assertTrue(result.consultExternal());
    }

    @Test
    void cacheFirst_deshabilitada_deberiaExterno() {
        var rule = new CacheFirstQueryRule();
        var request = new KnowledgeRequest("tema", Set.of(), List.of(), 5, Duration.ofHours(1));

        var result = rule.evaluate(request, QueryPolicyConfig.testing());

        assertTrue(result.consultExternal());
    }

    @Test
    void result_modoPorDefectoYAccesores() {
        var result = new QueryPolicyResult(null, null);

        assertEquals(QueryMode.EXTERNAL, result.mode());
        assertTrue(result.reasons().isEmpty());
        assertTrue(result.consultExternal());
        assertFalse(result.cacheOnly());
    }

    @Test
    void mode_displayNames() {
        assertEquals("Solo modelo", QueryMode.MODEL_ONLY.displayName());
        assertEquals("Solo caché", QueryMode.CACHE_ONLY.displayName());
        assertEquals("Consulta externa", QueryMode.EXTERNAL.displayName());
    }

    @Test
    void category_displayNames() {
        assertEquals("Consulta", PolicyCategory.QUERY.displayName());
        assertEquals("Proveedores", PolicyCategory.PROVIDER.displayName());
        assertEquals("Calidad", PolicyCategory.QUALITY.displayName());
        assertEquals("Costo", PolicyCategory.COST.displayName());
        assertEquals("Contexto", PolicyCategory.CONTEXT.displayName());
    }
}
