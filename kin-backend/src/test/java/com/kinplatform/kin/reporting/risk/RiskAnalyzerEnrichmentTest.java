package com.kinplatform.kin.reporting.risk;

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

class RiskAnalyzerEnrichmentTest {

    @Test
    void market_deberiaIncluirRiesgoEnriquecido_cuandoHayEvidencia() {
        var analyzer = new MarketRiskAnalyzer();
        var risks = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.MARKET, "Mercado saturado", "src-market")));

        assertEquals(1, risks.size());
        assertTrue(hasRule(risks, "ENRIQUECIDO_MERCADO"));
        assertTrue(risks.get(0).explanation().evidence().contains("Mercado saturado"));
    }

    @Test
    void market_sinEvidencia_deberiaComportarseComoHoy() {
        var analyzer = new MarketRiskAnalyzer();
        var risks = analyzer.analyze(input(EnrichmentResult.empty()));

        assertTrue(risks.isEmpty());
        assertFalse(hasRule(risks, "ENRIQUECIDO_MERCADO"));
    }

    @Test
    void market_deberiaIgnorarEvidenciaDeOtraCategoria() {
        var analyzer = new MarketRiskAnalyzer();
        var risks = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.FINANCIAL, "Costo creciente", "src-fin")));

        assertTrue(risks.isEmpty());
    }

    @Test
    void financial_deberiaIncluirRiesgoEnriquecido_cuandoHayEvidencia() {
        var analyzer = new FinancialRiskAnalyzer();
        var risks = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.FINANCIAL, "Margen en caída", "src-fin")));

        assertEquals(1, risks.size());
        assertTrue(hasRule(risks, "ENRIQUECIDO_FINANCIERO"));
        assertTrue(risks.get(0).explanation().evidence().contains("Margen en caída"));
    }

    @Test
    void financial_sinEvidencia_deberiaComportarseComoHoy() {
        var analyzer = new FinancialRiskAnalyzer();
        var risks = analyzer.analyze(input(EnrichmentResult.empty()));

        assertTrue(risks.isEmpty());
        assertFalse(hasRule(risks, "ENRIQUECIDO_FINANCIERO"));
    }

    @Test
    void business_deberiaIncluirRiesgoEnriquecido_cuandoHayEvidencia() {
        var analyzer = new BusinessRiskAnalyzer();
        var risks = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.COMPETITIVE, "Barrera de entrada alta", "src-comp")));

        assertEquals(1, risks.size());
        assertTrue(hasRule(risks, "ENRIQUECIDO_COMPETITIVO"));
        assertTrue(risks.get(0).explanation().evidence().contains("Barrera de entrada alta"));
    }

    @Test
    void business_sinEvidencia_deberiaComportarseComoHoy() {
        var analyzer = new BusinessRiskAnalyzer();
        var risks = analyzer.analyze(input(EnrichmentResult.empty()));

        assertTrue(risks.isEmpty());
        assertFalse(hasRule(risks, "ENRIQUECIDO_COMPETITIVO"));
    }

    @Test
    void technical_deberiaIncluirRiesgoEnriquecido_cuandoHayEvidencia() {
        var analyzer = new TechnicalRiskAnalyzer();
        var risks = analyzer.analyze(input(
            enrichmentFor(EvidenceCategory.INNOVATION, "Tecnología en disrupción", "src-tech")));

        assertEquals(1, risks.size());
        assertTrue(hasRule(risks, "ENRIQUECIDO_INNOVACION"));
        assertTrue(risks.get(0).explanation().evidence().contains("Tecnología en disrupción"));
    }

    @Test
    void technical_sinEvidencia_deberiaComportarseComoHoy() {
        var analyzer = new TechnicalRiskAnalyzer();
        var risks = analyzer.analyze(input(EnrichmentResult.empty()));

        assertTrue(risks.isEmpty());
        assertFalse(hasRule(risks, "ENRIQUECIDO_INNOVACION"));
    }

    private RiskInput input(EnrichmentResult enrichment) {
        return new RiskInput(project(), evaluation(),
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

    private static boolean hasRule(List<Risk> risks, String rule) {
        return risks.stream().anyMatch(r -> r.appliedRules().contains(rule));
    }
}
