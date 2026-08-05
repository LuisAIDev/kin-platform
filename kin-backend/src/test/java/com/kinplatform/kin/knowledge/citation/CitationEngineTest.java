package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationEngineTest {

    private final CitationEngine engine = new CitationEngine();

    private KnowledgeResult result(KnowledgeFact... facts) {
        return new KnowledgeResult(List.of(facts), List.of(), List.of(), 1.0, "ok", "KnowledgeEngine", "v1");
    }

    private KnowledgeFact fact(String sourceId, String url, SourceTrust trust) {
        return KnowledgeFact.of("Dato verificado de mercado.", sourceId, url,
            OffsetDateTime.now().minusDays(5), trust, "MERCADO");
    }

    @Test
    void unaFuente_deberiaProducirUnaCita() {
        var result = result(fact("src-1", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC));

        var citation = engine.produce(result, null, CitationStyle.INLINE);

        assertFalse(citation.isEmpty());
        assertEquals(1, citation.bundle().entries().size());
        assertEquals(1, citation.bundle().references().size());
        assertEquals("(src-1, " + OffsetDateTime.now().minusDays(5).getYear() + ")",
            citation.bundle().references().get(0));
        assertEquals(1, citation.decisions().size());
    }

    @Test
    void multiplesFuentes_deberianCitarseTodas() {
        var result = result(
            fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC),
            fact("b", "https://example.com/b", SourceTrust.SECONDARY));

        var citation = engine.produce(result, null, CitationStyle.FOOTNOTE);

        assertEquals(2, citation.bundle().entries().size());
        assertEquals(2, citation.bundle().references().size());
        assertTrue(citation.bundle().references().get(0).startsWith("[1]"));
        assertTrue(citation.bundle().references().get(1).startsWith("[2]"));
    }

    @Test
    void confianzaBaja_deberiaExcluirseConPoliticaEstricta() {
        var result = result(
            fact("a", "https://example.com/a", SourceTrust.UNVERIFIED),
            fact("b", "https://example.com/b", SourceTrust.OFFICIAL_PUBLIC));
        var policy = new VerifiedCitationPolicy(0.8);

        var citation = engine.produce(result, policy, CitationStyle.INLINE);

        assertEquals(1, citation.bundle().entries().size());
        assertEquals("b", citation.bundle().entries().get(0).sourceId());
        assertEquals(2, citation.decisions().size());
        assertFalse(citation.decisions().get(0).included());
    }

    @Test
    void confianzaAlta_deberiaReflejarseEnScore() {
        var result = result(
            fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC),
            fact("b", "https://example.com/b", SourceTrust.OFFICIAL_PUBLIC));

        var citation = engine.produce(result, null, CitationStyle.APPENDIX);

        assertEquals(1.0, citation.bundle().score(), 1e-9);
        assertEquals(1.0, citation.bundle().metadata().topConfidence(), 1e-9);
        assertEquals(1.0, citation.bundle().metadata().averageConfidence(), 1e-9);
    }

    @Test
    void fuentesDuplicadas_deberianDeduplicarse() {
        var result = result(
            fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC),
            fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC));

        var citation = engine.produce(result, null, CitationStyle.INLINE);

        assertEquals(1, citation.bundle().entries().size());
        assertEquals(1, citation.bundle().metadata().count());
    }

    @Test
    void sinFuentes_deberiaDevolverBundleVacio() {
        var citation = engine.produce(result(), null, CitationStyle.INLINE);

        assertTrue(citation.isEmpty());
        assertTrue(citation.bundle().explanation().contains("Sin hechos"));
        assertEquals(0.0, citation.bundle().score(), 1e-9);
    }

    @Test
    void resultNulo_deberiaDevolverBundleVacio() {
        var citation = engine.produce(null, null, CitationStyle.INLINE);

        assertTrue(citation.isEmpty());
    }

    @Test
    void estiloDisabled_deberiaCortarAntesDeEvaluar() {
        var result = result(fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC));

        var citation = engine.produce(result, null, CitationStyle.DISABLED);

        assertTrue(citation.isEmpty());
        assertTrue(citation.bundle().explanation().contains("deshabilitada"));
    }

    @Test
    void estiloHidden_deberiaConservarCitasSinReferencias() {
        var result = result(fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC));

        var citation = engine.produce(result, null, CitationStyle.HIDDEN);

        assertFalse(citation.isEmpty());
        assertEquals(1, citation.bundle().entries().size());
        assertTrue(citation.bundle().references().isEmpty());
    }

    @Test
    void offline_deberiaFuncionarSinRed() {
        var result = result(fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC));

        var citation = engine.produce(result, null, CitationStyle.FOOTNOTE);

        assertFalse(citation.isEmpty());
        assertEquals(1, citation.bundle().entries().size());
    }

    @Test
    void sinSourceMetadata_deberiaExcluirse() {
        var result = result(
            KnowledgeFact.of("Dato.", "", "", OffsetDateTime.now().minusDays(5),
                SourceTrust.OFFICIAL_PUBLIC, "MERCADO"));

        var citation = engine.produce(result, null, CitationStyle.INLINE);

        assertTrue(citation.isEmpty());
        assertTrue(citation.decisions().get(0).reason().contains("SourceMetadata"));
    }

    @Test
    void determinismo_mismaEntradaMismoResultado() {
        var result = result(
            fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC),
            fact("b", "https://example.com/b", SourceTrust.SECONDARY));

        var r1 = engine.produce(result, null, CitationStyle.APPENDIX);
        var r2 = engine.produce(result, null, CitationStyle.APPENDIX);

        assertEquals(r1, r2);
    }

    @Test
    void estiloNulo_deberiaUsarInline() {
        var result = result(fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC));

        var citation = engine.produce(result, null, null);

        assertEquals(CitationStyle.INLINE, citation.bundle().style());
    }

    @Test
    void explicacion_deberiaReflejarConteoYConfianza() {
        var result = result(
            fact("a", "https://example.com/a", SourceTrust.OFFICIAL_PUBLIC),
            fact("b", "https://example.com/b", SourceTrust.SECONDARY));

        var citation = engine.produce(result, null, CitationStyle.INLINE);

        assertTrue(citation.bundle().explanation().contains("2 de 2"));
    }
}
