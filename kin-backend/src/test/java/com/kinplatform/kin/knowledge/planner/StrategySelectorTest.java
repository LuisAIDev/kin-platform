package com.kinplatform.kin.knowledge.planner;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.policy.QueryPolicyConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrategySelectorTest {

    private final StrategySelector selector = new StrategySelector();

    private QueryClassification classification(IntentType type, IntentFacet... facets) {
        var domainMapping = FacetDomainMapping.defaults();
        var order = FacetOrder.defaults();
        var facetSet = Set.copyOf(java.util.Arrays.asList(facets));
        var domains = new java.util.LinkedHashSet<QueryDomain>();
        for (var facet : facetSet) {
            domains.add(domainMapping.domainOf(facet));
        }
        IntentFacet primary = order.primaryOf(facetSet).orElseThrow();
        return new QueryClassification(type, domainMapping.domainOf(primary),
            Set.copyOf(domains), facetSet, 20 + facetSet.size());
    }

    private KnowledgeRequest request(String topic) {
        return KnowledgeRequest.of(topic, List.of());
    }

    @Test
    void clasificacionNula_deberiaSerSingle() {
        assertEquals(QueryStrategy.SINGLE, selector.select(null, request("tema")));
    }

    @Test
    void conocimientoEstable_deberiaSerSingle() {
        var classification = new QueryClassification(IntentType.CONOCIMIENTO_ESTABLE,
            QueryDomain.STABLE, Set.of(QueryDomain.STABLE), Set.of(), 0);

        assertEquals(QueryStrategy.SINGLE, selector.select(classification, request("scrum")));
    }

    @Test
    void modoModeloPuro_deberiaSerSingle() {
        assertEquals(QueryStrategy.SINGLE,
            selector.select(QueryClassification.general(), request("scrum")));
    }

    @Test
    void modoCache_deberiaSerCached() {
        var request = new KnowledgeRequest("tema", Set.of(), List.of(), 5, Duration.ofHours(6));

        assertEquals(QueryStrategy.CACHED,
            selector.select(QueryClassification.general(), request));
    }

    @Test
    void soloDocumento_deberiaSerLocalOnly() {
        var classification = classification(IntentType.DOCUMENTO, IntentFacet.DOCUMENTO);

        assertEquals(QueryStrategy.LOCAL_ONLY, selector.select(classification, request("pdf")));
    }

    @Test
    void tendenciasUnica_deberiaSerInternetOnly() {
        var classification = classification(IntentType.TENDENCIAS, IntentFacet.TENDENCIAS);

        assertEquals(QueryStrategy.INTERNET_ONLY, selector.select(classification, request("tendencias")));
    }

    @Test
    void facetaExternaUnica_deberiaSerSingle() {
        var classification = classification(IntentType.MERCADO, IntentFacet.MERCADO);

        assertEquals(QueryStrategy.SINGLE, selector.select(classification, request("mercado")));
    }

    @Test
    void localMasExterna_deberiaSerHybrid() {
        var classification = classification(IntentType.MERCADO, IntentFacet.DOCUMENTO, IntentFacet.MERCADO);

        assertEquals(QueryStrategy.HYBRID, selector.select(classification, request("pdf y mercado")));
    }

    @Test
    void multiplesDominios_deberiaSerHybrid() {
        var classification = classification(IntentType.MERCADO, IntentFacet.MERCADO, IntentFacet.ESTADISTICA);

        assertEquals(QueryStrategy.HYBRID, selector.select(classification, request("mercado del café")));
    }

    @Test
    void regulatoriaYLegal_deberiaSerSequential() {
        var classification = classification(IntentType.REGULATORIA, IntentFacet.REGULATORIA, IntentFacet.LEGAL);

        assertEquals(QueryStrategy.SEQUENTIAL, selector.select(classification, request("sas")));
    }

    @Test
    void dosFacetasMismoDominio_deberiaSerMulti() {
        var classification = classification(IntentType.MERCADO, IntentFacet.TENDENCIAS, IntentFacet.COMPETENCIA);

        assertEquals(QueryStrategy.MULTI, selector.select(classification, request("tendencias y competencia")));
    }

    @Test
    void tresFacetasMismoDominio_deberiaSerParallel() {
        var classification = classification(IntentType.MERCADO,
            IntentFacet.MERCADO, IntentFacet.COMPETENCIA, IntentFacet.TENDENCIAS);

        assertEquals(QueryStrategy.PARALLEL, selector.select(classification, request("mercado y competencia")));
    }

    @Test
    void intencionGeneral_deberiaSerSingle() {
        assertEquals(QueryStrategy.SINGLE, selector.select(QueryClassification.general(), request("hola")));
    }

    @Test
    void cacheFirstConVentanaAmplia_deberiaConsultarExterno() {
        var request = new KnowledgeRequest("tema", Set.of(), List.of(), 5, Duration.ofDays(60));

        assertEquals(QueryStrategy.SINGLE,
            selector.select(QueryClassification.general(), request));
    }

    @Test
    void cacheDeshabilitadaPorConfig_deberiaConsultarExterno() {
        var config = QueryPolicyConfig.testing();
        var selector = new StrategySelector(new com.kinplatform.kin.knowledge.policy.KnowledgePolicyEngine(), config);
        var request = new KnowledgeRequest("tema", Set.of(), List.of(), 5, Duration.ofHours(1));

        assertEquals(QueryStrategy.SINGLE, selector.select(QueryClassification.general(), request));
    }
}
