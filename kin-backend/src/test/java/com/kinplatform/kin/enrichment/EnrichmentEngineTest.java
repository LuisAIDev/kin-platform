package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrichmentEngineTest {

    private final EnrichmentEngine engine = new EnrichmentEngine(new FactRanker());

    private KnowledgeResult knowledge(KnowledgeFact... facts) {
        return new KnowledgeResult(List.of(facts),
            java.util.Arrays.stream(facts).map(KnowledgeFact::sourceId).toList(),
            List.of(), 0.8, "conocimiento", "KnowledgeEngine", "v1");
    }

    private KnowledgeFact fact(String claim, String category, SourceTrust trust, String sourceId) {
        return KnowledgeFact.of(claim, sourceId, "https://example.com", null, trust, category);
    }

    private EnrichmentInput input(KnowledgeResult knowledge) {
        return EnrichmentInput.of(ProjectContext.fromProject("P", "D", "C"), knowledge);
    }

    @Test
    void metadata_deberiaDeclararFaseTipoYPrioridadCorrectos() {
        var metadata = engine.metadata();

        assertEquals(EnrichmentEngine.GENERATOR_NAME, metadata.name());
        assertEquals("v1", metadata.version());
        assertEquals("KIN Architecture Team", metadata.author());
        assertEquals(EnginePhase.ANALYSIS, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
        assertEquals(55, metadata.priority());
        assertTrue(metadata.dependencies().isEmpty());
    }

    @Test
    void evaluate_deberiaProducirResultadoEnriquecido() {
        var k = knowledge(
            fact("El mercado retail crece con demanda", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"));

        var result = engine.evaluate(input(k));

        assertFalse(result.isEmpty());
        assertEquals(1, result.totalEvidence());
        assertEquals(1, result.rankCount());
        assertEquals(List.of("src-1"), result.sourcesUsed());
        assertEquals(EnrichmentEngine.GENERATOR_NAME, result.generatedBy());
        assertEquals(EnrichmentEngine.ENGINE_VERSION, result.engineVersion());
    }

    @Test
    void evaluate_conConocimientoVacio_deberiaRetornarVacio() {
        var result = engine.evaluate(input(KnowledgeResult.empty()));

        assertTrue(result.isEmpty());
        assertEquals(0.0, result.confidence(), 1e-9);
    }

    @Test
    void evaluate_conEntradaNula_deberiaRetornarVacio() {
        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void evaluate_conContextoNulo_deberiaRetornarVacio() {
        var k = knowledge(fact("El mercado retail crece", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"));
        var input = new EnrichmentInput(null, k, null, 0.0);

        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_conRanquerNulo_deberiaRetornarVacio() {
        var engineSinRanquer = new EnrichmentEngine(null);
        var k = knowledge(fact("El mercado retail crece", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"));

        assertTrue(engineSinRanquer.evaluate(input(k)).isEmpty());
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        var k = knowledge(
            fact("El mercado retail crece con demanda", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"),
            fact("Barrera de entrada baja", "competencia", SourceTrust.SECONDARY, "src-2"));

        var r1 = engine.evaluate(input(k));
        var r2 = engine.evaluate(input(k));

        assertEquals(r1.confidence(), r2.confidence(), 1e-9);
        assertEquals(r1.ranks(), r2.ranks());
        assertEquals(r1.explanation(), r2.explanation());
    }

    @Test
    void evaluate_deberiaConsolidarFuentesDistintas() {
        var k = knowledge(
            fact("El mercado retail crece", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"),
            fact("Nueva demanda en el mercado", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"),
            fact("Barrera de entrada baja", "competencia", SourceTrust.OFFICIAL_PUBLIC, "src-2"));

        var result = engine.evaluate(input(k));

        assertEquals(3, result.totalEvidence());
        assertEquals(2, result.sourcesUsed().size());
        assertEquals(2, result.rankCount());
    }

    @Test
    void evaluate_deberiaGenerarExplicacionConEvidencia() {
        var k = knowledge(fact("El mercado retail crece", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"));

        var result = engine.evaluate(input(k));

        assertTrue(result.explanation().contains("evidencia"));
        assertEquals(1, result.totalEvidence());
    }

    @Test
    void evaluate_deberiaRespetarCategoriasSolicitadas() {
        var k = knowledge(
            fact("El mercado retail crece", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"),
            fact("Rentabilidad y margen del sector", "financiero", SourceTrust.OFFICIAL_PUBLIC, "src-2"));

        var input = EnrichmentInput.of(ProjectContext.fromProject("P", "D", "C"), k,
            Set.of(EvidenceCategory.FINANCIAL), 0.0);
        var result = engine.evaluate(input);

        assertEquals(1, result.rankCount());
        assertTrue(result.rankFor(EvidenceCategory.FINANCIAL).isPresent());
        assertFalse(result.rankFor(EvidenceCategory.MARKET).isPresent());
    }
}
