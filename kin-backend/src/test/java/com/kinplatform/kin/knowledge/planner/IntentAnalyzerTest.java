package com.kinplatform.kin.knowledge.planner;

import com.kinplatform.kin.knowledge.KnowledgeRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentAnalyzerTest {

    private final IntentAnalyzer analyzer = new IntentAnalyzer();

    @Test
    void scrum_deberiaSerConocimientoEstable() {
        var intent = analyzer.analyze(KnowledgeRequest.of("Explícame Scrum", List.of()));

        assertEquals(IntentType.CONOCIMIENTO_ESTABLE, intent.type());
        assertTrue(intent.facets().isEmpty());
    }

    @Test
    void crearSas_deberiaProducirRegulatoriaYLegal() {
        var intent = analyzer.analyze(KnowledgeRequest.of("Quiero crear una SAS en Colombia", List.of()));

        assertEquals(IntentType.REGULATORIA, intent.type());
        assertTrue(intent.facets().contains(IntentFacet.REGULATORIA));
        assertTrue(intent.facets().contains(IntentFacet.LEGAL));
    }

    @Test
    void panaderia_deberiaProducirMultiplesFacetas() {
        var intent = analyzer.analyze(KnowledgeRequest.of("Abrir panadería en Cartagena", List.of()));

        assertTrue(intent.facets().contains(IntentFacet.REGULATORIA));
        assertTrue(intent.facets().contains(IntentFacet.MERCADO));
        assertTrue(intent.facets().contains(IntentFacet.ESTADISTICA));
        assertTrue(intent.facets().size() >= 3);
        assertEquals(IntentType.REGULATORIA, intent.type());
    }

    @Test
    void pdf_deberiaSerDocumento() {
        var intent = analyzer.analyze(KnowledgeRequest.of("Analiza este PDF", List.of()));

        assertEquals(IntentType.DOCUMENTO, intent.type());
        assertTrue(intent.facets().contains(IntentFacet.DOCUMENTO));
    }

    @Test
    void cafe_deberiaProducirMercadoYEstadistica() {
        var intent = analyzer.analyze(KnowledgeRequest.of("¿Cómo está el mercado del café colombiano?", List.of()));

        assertTrue(intent.facets().contains(IntentFacet.MERCADO));
        assertTrue(intent.facets().contains(IntentFacet.ESTADISTICA));
    }

    @Test
    void requestNulo_deberiaGeneral() {
        var intent = analyzer.analyze(null);

        assertEquals(IntentType.GENERAL, intent.type());
        assertTrue(intent.facets().isEmpty());
    }

    @Test
    void requestVacio_deberiaGeneral() {
        var intent = analyzer.analyze(KnowledgeRequest.of("   ", List.of()));

        assertEquals(IntentType.GENERAL, intent.type());
    }

    @Test
    void textoSinIntencionReconocible_deberiaGeneral() {
        var intent = analyzer.analyze(KnowledgeRequest.of("crear una casa", List.of()));

        assertEquals(IntentType.GENERAL, intent.type());
    }

    @Test
    void palabrasClave_deberianContribuirAlAnalisis() {
        var intent = analyzer.analyze(KnowledgeRequest.of("Proyecto nuevo", List.of("scrum")));

        assertEquals(IntentType.CONOCIMIENTO_ESTABLE, intent.type());
    }

    @Test
    void reglasRegistrables_deberianReemplazarLasPorDefecto() {
        var custom = new IntentAnalyzer(
            List.of(KeywordIntentRule.of(IntentFacet.TENDENCIAS, "panaderia")),
            FacetImplication.defaults(), FacetOrder.defaults());

        var intent = custom.analyze(KnowledgeRequest.of("panadería", List.of()));

        assertEquals(IntentType.TENDENCIAS, intent.type());
        assertTrue(intent.facets().contains(IntentFacet.TENDENCIAS));
        assertFalse(intent.facets().contains(IntentFacet.MERCADO));
    }

    @Test
    void sinImplicaciones_deberiaMantenerFacetasDirectas() {
        var without = new IntentAnalyzer(
            List.of(KeywordIntentRule.of(IntentFacet.MERCADO, "mercado")),
            new FacetImplication(Map.of()), FacetOrder.defaults());

        var intent = without.analyze(KnowledgeRequest.of("mercado", List.of()));

        assertTrue(intent.facets().contains(IntentFacet.MERCADO));
        assertFalse(intent.facets().contains(IntentFacet.ESTADISTICA));
    }

    @Test
    void normalizacionDeAcentos_deberiaCoincidirConPalabrasSinAcentos() {
        var intent = analyzer.analyze(KnowledgeRequest.of("Abrir panadería en Cartagena", List.of()));

        assertTrue(intent.facets().contains(IntentFacet.MERCADO));
    }

    @Test
    void tokenDeUnSoloCaracterNoDebeCoincidirPorSubcadena() {
        assertFalse(KeywordIntentRule.of(IntentFacet.LEGAL, "sas")
            .matches("mi casa es grande"));
    }

    @Test
    void fraseMultiPalabra_deberiaCoincidirPorSubcadena() {
        assertTrue(KeywordIntentRule.stable("x", "metodologia agil")
            .matches("aplicar metodologia agil hoy"));
    }

    @Test
    void reglaConTextoNuloOVacio_deberiaNoCoincidir() {
        var rule = KeywordIntentRule.of(IntentFacet.MERCADO, "mercado");

        assertFalse(rule.matches(null));
        assertFalse(rule.matches("   "));
    }
}
