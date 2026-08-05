package com.kinplatform.kin.knowledge.planner;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanGeneratorTest {

    private final PlanGenerator generator = new PlanGenerator();

    private QueryClassification classification(IntentType type, IntentFacet... facets) {
        var mapping = FacetDomainMapping.defaults();
        var order = FacetOrder.defaults();
        var set = Set.copyOf(java.util.Arrays.asList(facets));
        var domains = new java.util.LinkedHashSet<QueryDomain>();
        for (var facet : set) {
            domains.add(mapping.domainOf(facet));
        }
        IntentFacet primary = order.primaryOf(set).orElseThrow();
        return new QueryClassification(type, mapping.domainOf(primary),
            Set.copyOf(domains), set, 20 + set.size());
    }

    private KnowledgeRequest request(String topic) {
        return KnowledgeRequest.of(topic, List.of());
    }

    @Test
    void conocimientoEstable_deberiaGenerarPlanVacio() {
        var classification = new QueryClassification(IntentType.CONOCIMIENTO_ESTABLE,
            QueryDomain.STABLE, Set.of(QueryDomain.STABLE), Set.of(), 0);

        var plan = generator.generate(classification, QueryStrategy.SINGLE, request("scrum"));

        assertTrue(plan.isEmpty());
        assertTrue(plan.reasons().stream().anyMatch(r -> r.contains("estable")));
    }

    @Test
    void estrategiaCache_deberiaGenerarPlanVacio() {
        var plan = generator.generate(QueryClassification.general(), QueryStrategy.CACHED, request("tema"));

        assertTrue(plan.isEmpty());
    }

    @Test
    void soloLocal_deberiaGenerarConsultaDeDocumento() {
        var classification = classification(IntentType.DOCUMENTO, IntentFacet.DOCUMENTO);

        var plan = generator.generate(classification, QueryStrategy.LOCAL_ONLY, request("pdf"));

        assertEquals(1, plan.queries().size());
        assertEquals(ProviderType.DOCUMENT, plan.queries().get(0).providerType());
        assertEquals(IntentFacet.DOCUMENTO, plan.queries().get(0).facet());
    }

    @Test
    void hybrid_deberiaDeduplicarPorTipoDeProveedor() {
        var classification = classification(IntentType.MERCADO, IntentFacet.MERCADO, IntentFacet.ESTADISTICA);

        var plan = generator.generate(classification, QueryStrategy.HYBRID, request("café"));

        assertEquals(1, plan.queries().size());
        assertEquals(ProviderType.STATISTICS, plan.queries().get(0).providerType());
    }

    @Test
    void multiFacetas_deberiaGenerarUnaConsultaPorTipo() {
        var classification = classification(IntentType.REGULATORIA,
            IntentFacet.REGULATORIA, IntentFacet.MERCADO, IntentFacet.ESTADISTICA);

        var plan = generator.generate(classification, QueryStrategy.HYBRID, request("panadería"));

        assertEquals(2, plan.queries().size());
        assertEquals(ProviderType.GOVERNMENT, plan.queries().get(0).providerType());
        assertEquals(ProviderType.STATISTICS, plan.queries().get(1).providerType());
    }

    @Test
    void single_deberiaConservarSoloLaFacetaPrimaria() {
        var classification = classification(IntentType.REGULATORIA, IntentFacet.REGULATORIA, IntentFacet.LEGAL);

        var plan = generator.generate(classification, QueryStrategy.SINGLE, request("sas"));

        assertEquals(1, plan.queries().size());
        assertEquals(IntentFacet.REGULATORIA, plan.queries().get(0).facet());
    }

    @Test
    void internetOnly_deberiaExcluirFacetasLocales() {
        var classification = classification(IntentType.MERCADO, IntentFacet.DOCUMENTO, IntentFacet.MERCADO);

        var plan = generator.generate(classification, QueryStrategy.INTERNET_ONLY, request("tema"));

        assertEquals(1, plan.queries().size());
        assertEquals(ProviderType.STATISTICS, plan.queries().get(0).providerType());
    }

    @Test
    void intencionGeneral_deberiaGenerarConsultaWebDeRespaldo() {
        var plan = generator.generate(QueryClassification.general(), QueryStrategy.SINGLE, request("hola"));

        assertEquals(1, plan.queries().size());
        assertEquals(ProviderType.WEB_SEARCH, plan.queries().get(0).providerType());
        assertNull(plan.queries().get(0).facet());
    }

    @Test
    void requestNulo_deberiaGenerarPlanSeguro() {
        var plan = generator.generate(QueryClassification.general(), QueryStrategy.SINGLE, null);

        assertEquals(1, plan.queries().size());
        assertEquals("", plan.queries().get(0).topic());
    }

    @Test
    void clasificacionNula_deberiaGenerarPlanGeneral() {
        var plan = generator.generate(null, QueryStrategy.SINGLE, request("tema"));

        assertEquals(1, plan.queries().size());
        assertEquals(ProviderType.WEB_SEARCH, plan.queries().get(0).providerType());
    }
}
