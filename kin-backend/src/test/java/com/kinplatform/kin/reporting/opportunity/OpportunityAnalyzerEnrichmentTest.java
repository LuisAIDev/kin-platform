package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.AnalysisResult;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.EvidenceRank;
import com.kinplatform.kin.enrichment.EvidenceScore;
import com.kinplatform.kin.enrichment.KnowledgeEvidence;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpportunityAnalyzerEnrichmentTest {

    @Test
    void market_deberiaIncluirOportunidadEnriquecida_cuandoHayEvidencia() {
        var analyzer = new MarketOpportunityAnalyzer();
        var opportunities = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.MARKET, "El mercado crece 15% anual", "src-market")));

        assertEquals(1, opportunities.size());
        assertTrue(hasRule(opportunities, "ENRIQUECIDO_MERCADO"));
        assertTrue(opportunities.get(0).explanation().evidence().contains("El mercado crece 15% anual"));
    }

    @Test
    void market_sinEvidencia_deberiaComportarseComoHoy() {
        var analyzer = new MarketOpportunityAnalyzer();
        var opportunities = analyzer.analyze(input(EnrichmentResult.empty()));

        assertTrue(opportunities.isEmpty());
        assertFalse(hasRule(opportunities, "ENRIQUECIDO_MERCADO"));
    }

    @Test
    void market_deberiaIgnorarEvidenciaDeOtraCategoria() {
        var analyzer = new MarketOpportunityAnalyzer();
        var opportunities = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.INNOVATION, "Nueva tecnología", "src-tech")));

        assertTrue(opportunities.isEmpty());
    }

    @Test
    void innovation_deberiaIncluirOportunidadEnriquecida_cuandoHayEvidencia() {
        var analyzer = new InnovationOpportunityAnalyzer();
        var opportunities = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.INNOVATION, "Patente de tecnología disruptiva", "src-inn")));

        assertEquals(1, opportunities.size());
        assertTrue(hasRule(opportunities, "ENRIQUECIDO_INNOVACION"));
        assertTrue(opportunities.get(0).explanation().evidence().contains("Patente de tecnología disruptiva"));
    }

    @Test
    void innovation_sinEvidencia_deberiaComportarseComoHoy() {
        var analyzer = new InnovationOpportunityAnalyzer();
        var opportunities = analyzer.analyze(input(EnrichmentResult.empty()));

        assertTrue(opportunities.isEmpty());
        assertFalse(hasRule(opportunities, "ENRIQUECIDO_INNOVACION"));
    }

    @Test
    void financial_deberiaIncluirOportunidadEnriquecida_cuandoHayEvidencia() {
        var analyzer = new FinancialOpportunityAnalyzer();
        var opportunities = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.FINANCIAL, "Tasa de interés en descenso", "src-fin")));

        assertEquals(1, opportunities.size());
        assertTrue(hasRule(opportunities, "ENRIQUECIDO_FINANCIERO"));
        assertTrue(opportunities.get(0).explanation().evidence().contains("Tasa de interés en descenso"));
    }

    @Test
    void financial_sinEvidencia_deberiaComportarseComoHoy() {
        var analyzer = new FinancialOpportunityAnalyzer();
        var opportunities = analyzer.analyze(input(EnrichmentResult.empty()));

        assertTrue(opportunities.isEmpty());
        assertFalse(hasRule(opportunities, "ENRIQUECIDO_FINANCIERO"));
    }

    @Test
    void competitive_deberiaIncluirOportunidadEnriquecida_cuandoHayEvidencia() {
        var analyzer = new CompetitiveOpportunityAnalyzer();
        var opportunities = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.COMPETITIVE, "Entrada de nuevos rivales", "src-comp")));

        assertEquals(1, opportunities.size());
        assertTrue(hasRule(opportunities, "ENRIQUECIDO_COMPETITIVO"));
        assertTrue(opportunities.get(0).explanation().evidence().contains("Entrada de nuevos rivales"));
    }

    @Test
    void competitive_sinEvidencia_deberiaComportarseComoHoy() {
        var analyzer = new CompetitiveOpportunityAnalyzer();
        var opportunities = analyzer.analyze(input(EnrichmentResult.empty()));

        assertTrue(opportunities.isEmpty());
        assertFalse(hasRule(opportunities, "ENRIQUECIDO_COMPETITIVO"));
    }

    private OpportunityInput input(EnrichmentResult enrichment) {
        return new OpportunityInput(project(), evaluation(),
            ConversationDecision.generateReport("reporte"), score(), enrichment);
    }

    private ProjectContext project() {
        var ctx = ProjectContext.fromProject("Proyecto Test", "Solución innovadora", "Tecnología");
        var extra = new LinkedHashMap<AnalyzedDimension, String>();
        extra.put(AnalyzedDimension.TARGET_CUSTOMER, "Jóvenes profesionales");
        extra.put(AnalyzedDimension.PROBLEM, "Falta de tiempo para planificar finanzas");
        extra.put(AnalyzedDimension.VALUE_PROPOSITION, "Ahorro de tiempo");
        extra.put(AnalyzedDimension.SOLUTION, "App de planificación");
        extra.put(AnalyzedDimension.REVENUE_MODEL, "Suscripción mensual");
        extra.put(AnalyzedDimension.COMPETITION, "Competidores directos e indirectos");
        extra.put(AnalyzedDimension.RISKS, "Riesgo regulatorio");
        extra.put(AnalyzedDimension.RESOURCES, "Dos desarrolladores");
        extra.put(AnalyzedDimension.MVP, "Versión mínima validable");
        extra.put(AnalyzedDimension.SCALABILITY, "Modelo SaaS");
        extra.put(AnalyzedDimension.OBJECTIVES, "Objetivos SMART");
        extra.put(AnalyzedDimension.CITY, "Lima");
        ctx.update(new AnalysisResult(extra));
        return ctx;
    }

    private CompletenessEvaluation evaluation() {
        return new CompletenessEvaluation(
            1.0, List.of(), List.of(), 0.8,
            CompletenessEvaluation.MaturityLevel.MATURE,
            CompletenessEvaluation.ViabilityLevel.HIGH, 0.9,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, AnalyzedDimension.values().length, AnalyzedDimension.values().length);
    }

    private ScoreResult score() {
        return new ScoreResult(60, 100, Map.of(), "MEDIA", List.of(), List.of(), "");
    }

    private static EnrichmentResult enrichmentFor(EvidenceCategory category, String claim,
                                                  String sourceId) {
        var fact = KnowledgeFact.of(claim, sourceId, "https://example.com/" + sourceId,
            OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "sector");
        var rank = EvidenceRank.of(category,
            List.of(new KnowledgeEvidence(fact, EvidenceScore.of(0.8, category, "Relevante."))));
        return new EnrichmentResult(List.of(rank), List.of(sourceId), 0.8,
            "enriquecido", "Test", "v1");
    }

    private static boolean hasRule(List<Opportunity> opportunities, String rule) {
        return opportunities.stream().anyMatch(o -> o.appliedRules().contains(rule));
    }
}
