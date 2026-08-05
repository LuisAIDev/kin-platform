package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.SourceValidation;
import com.kinplatform.kin.knowledge.orchestrator.RankedCandidate;
import com.kinplatform.kin.knowledge.planner.ProviderType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationComponentsTest {

    private KnowledgeCandidate candidate(String sourceId, String sourceType, String content) {
        return new KnowledgeCandidate(content, sourceId, "Fuente", "https://example.com/report",
            OffsetDateTime.now().minusDays(5), "application/json",
            Map.of(SourceValidator.META_SOURCE_TYPE, sourceType));
    }

    private SourceValidation accepted(SourceTrust trust) {
        return SourceValidation.accepted(trust);
    }

    @Test
    void sourceRegistryAdapter_deberiaResolverTodasLasFuentes() {
        var source = new StubKnowledgeSource();
        var adapter = new SourceRegistryAdapter(new SourceRegistry(List.of(source)));

        var resolved = adapter.sourcesFor(ProviderType.STATISTICS);

        assertEquals(1, resolved.size());
        assertTrue(resolved.contains(source));
        assertTrue(new SourceRegistryAdapter(null).sourcesFor(ProviderType.GOVERNMENT).isEmpty());
    }

    @Test
    void sourceValidatorAdapter_deberiaDelegarLaValidacion() {
        var validator = new SourceValidator(Set.of("example.com"), Duration.ofDays(365),
            Set.of("application/json"));
        var adapter = new SourceValidatorAdapter(validator);

        var candidate = candidate("src-1", "official", "Contenido verificado.");
        var validations = adapter.validateAll(List.of(candidate));

        assertEquals(1, validations.size());
        assertTrue(validations.get(0).accepted());
        assertEquals(SourceTrust.OFFICIAL_PUBLIC, validations.get(0).trust());
        assertTrue(new SourceValidatorAdapter(null).validateAll(null).isEmpty());
    }

    @Test
    void ranker_deberiaOrdenarPorConfianzaDescendente() {
        var ranker = new DomainContextRanker();
        var unverified = new RankedCandidate(candidate("a", "unverified", "Contenido."),
            accepted(SourceTrust.UNVERIFIED));
        var official = new RankedCandidate(candidate("b", "official", "Contenido."),
            accepted(SourceTrust.OFFICIAL_PUBLIC));
        var secondary = new RankedCandidate(candidate("c", "secondary", "Contenido."),
            accepted(SourceTrust.SECONDARY));
        var rejected = new RankedCandidate(candidate("d", "official", "Contenido."),
            SourceValidation.rejected("rechazado"));

        var ranked = ranker.rank(List.of(unverified, rejected, official, secondary));

        assertEquals("b", ranked.get(0).candidate().sourceId());
        assertEquals("c", ranked.get(1).candidate().sourceId());
        assertEquals("a", ranked.get(2).candidate().sourceId());
        assertEquals("d", ranked.get(3).candidate().sourceId());
        assertTrue(ranker.rank(null).isEmpty());
        assertTrue(ranker.rank(List.of()).isEmpty());
    }

    @Test
    void assembler_deberiaProducirResultadoConHechosYConfianza() {
        var assembler = new DomainContextAssembler();
        var official = new RankedCandidate(candidate("src-1", "official", longContent()),
            accepted(SourceTrust.OFFICIAL_PUBLIC));

        var result = assembler.assemble(KnowledgeQuery.from(com.kinplatform.kin.knowledge.KnowledgeRequest.of(
            "mercado retail", List.of("retail"))), List.of(official));

        assertFalse(result.isEmpty());
        assertEquals(1, result.factCount());
        assertEquals(1.0, result.confidence(), 1e-9);
        assertEquals(KnowledgeEngine.GENERATOR_NAME, result.generatedBy());
        assertTrue(result.explanation().contains("1 de 1"));
    }

    @Test
    void assembler_deberiaDegradarConCandidatosRechazados() {
        var assembler = new DomainContextAssembler();
        var rejected = new RankedCandidate(candidate("src-1", "official", "Contenido."),
            SourceValidation.rejected("Protocolo HTTPS obligatorio"));

        var result = assembler.assemble(KnowledgeQuery.from(com.kinplatform.kin.knowledge.KnowledgeRequest.of(
            "mercado retail", List.of())), List.of(rejected));

        assertTrue(result.isEmpty());
        assertEquals(1, result.validations().size());
        assertFalse(result.validations().get(0).accepted());
        assertEquals(0.0, result.confidence(), 1e-9);
    }

    @Test
    void assembler_sinCandidatos_deberiaDevolverVacioConMotivo() {
        var assembler = new DomainContextAssembler("Gen", "v9");

        var result = assembler.assemble(KnowledgeQuery.from(com.kinplatform.kin.knowledge.KnowledgeRequest.of(
            "x", List.of())), List.of());

        assertTrue(result.isEmpty());
        assertTrue(result.explanation().contains("No se obtuvieron candidatos"));
        assertEquals("Gen", result.generatedBy());
        assertEquals("v9", result.engineVersion());
    }

    @Test
    void assembler_emptyResult_deberiaEstamparMetadatos() {
        var assembler = new DomainContextAssembler();

        var result = assembler.emptyResult("motivo");

        assertTrue(result.isEmpty());
        assertEquals("motivo", result.explanation());
        assertEquals(KnowledgeEngine.GENERATOR_NAME, result.generatedBy());
        assertEquals(KnowledgeEngine.ENGINE_VERSION, result.engineVersion());
    }

    private static String longContent() {
        return "Dato verificado de mercado. ".repeat(12);
    }

    private static final class StubKnowledgeSource implements com.kinplatform.kin.knowledge.KnowledgeSource {
        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            return List.of();
        }
    }
}
