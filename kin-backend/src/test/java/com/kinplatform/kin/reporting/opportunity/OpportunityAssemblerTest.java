package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpportunityAssemblerTest {

    private final OpportunityAssembler assembler = new OpportunityAssembler();

    private CompletenessEvaluation evaluation(double coverage, double quality,
                                              List<AnalyzedDimension> criticalMissing,
                                              List<String> signals) {
        return new CompletenessEvaluation(
            coverage, List.of(), criticalMissing, 0.7,
            CompletenessEvaluation.MaturityLevel.DEVELOPING,
            CompletenessEvaluation.ViabilityLevel.MEDIUM, quality,
            List.of(), signals, List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, 8, AnalyzedDimension.values().length);
    }

    @Test
    void build_deberiaConstruirOportunidadCompleta() {
        var opp = assembler.build(OpportunityCategory.MERCADO, "Título", "Descripción",
            2, 2, 0, ImpactLevel.HIGH, EffortLevel.MEDIUM,
            List.of("RULE_1"), AnalyzedDimension.PROBLEM, "Motivo",
            "Evidencia", evaluation(0.5, 0.7, List.of(), List.of()), "v1");

        assertEquals(OpportunityCategory.MERCADO, opp.category());
        assertEquals("Título", opp.title());
        assertEquals("Descripción", opp.description());
        assertEquals(4, opp.priority());
        assertEquals(AnalyzedDimension.PROBLEM, opp.relatedDimension());
        assertEquals(List.of("RULE_1"), opp.appliedRules());
        assertEquals("v1", opp.engineVersion());
        assertTrue(opp.explanation().appliedRule().contains("RULE_1"));
        assertTrue(opp.explanation().reason().contains("Motivo"));
        assertTrue(opp.explanation().evidence().contains("Evidencia"));
    }

    @Test
    void build_deberiaCalcularConfianzaConLaFormulaCompartida() {
        var opp = assembler.build(OpportunityCategory.MERCADO, "Título", "Descripción",
            2, 2, 0, ImpactLevel.HIGH, EffortLevel.MEDIUM,
            List.of("RULE_1"), AnalyzedDimension.PROBLEM, "Motivo",
            "Evidencia", evaluation(0.5, 0.7, List.of(), List.of()), "v1");

        double expected = 0.35 + 0.35 * 0.5 + 0.3 * 0.7;
        assertEquals(expected, opp.confidence(), 0.0001);
    }

    @Test
    void build_deberiaAcotarConfianzaEntreCeroYUno() {
        var full = assembler.build(OpportunityCategory.MERCADO, "A", "B",
            2, 2, 0, ImpactLevel.HIGH, EffortLevel.MEDIUM,
            List.of("R"), AnalyzedDimension.SECTOR, "M", "E",
            evaluation(1.0, 1.0, List.of(), List.of()), "v1");
        assertEquals(1.0, full.confidence(), 0.0001);

        var none = assembler.build(OpportunityCategory.MERCADO, "A", "B",
            2, 2, 0, ImpactLevel.HIGH, EffortLevel.MEDIUM,
            List.of("R"), AnalyzedDimension.SECTOR, "M", "E",
            evaluation(0.0, 0.0, List.of(), List.of()), "v1");
        assertEquals(0.35, none.confidence(), 0.0001);
    }

    @Test
    void build_deberiaAcotarPrioridadEntreUnoYDiez() {
        var tooHigh = assembler.build(OpportunityCategory.MERCADO, "A", "B",
            10, 3, 2, ImpactLevel.HIGH, EffortLevel.MEDIUM,
            List.of("R"), AnalyzedDimension.SECTOR, "M", "E",
            evaluation(0.5, 0.7, List.of(), List.of()), "v1");
        assertEquals(10, tooHigh.priority());

        var tooLow = assembler.build(OpportunityCategory.MERCADO, "A", "B",
            0, 0, 0, ImpactLevel.HIGH, EffortLevel.MEDIUM,
            List.of("R"), AnalyzedDimension.SECTOR, "M", "E",
            evaluation(0.5, 0.7, List.of(), List.of()), "v1");
        assertEquals(1, tooLow.priority());
    }

    @Test
    void build_deberiaIncluirCoberturaEnLaExplicacion() {
        var opp = assembler.build(OpportunityCategory.TECNOLOGICA, "Título", "Descripción",
            2, 2, 0, ImpactLevel.HIGH, EffortLevel.HIGH,
            List.of("RULE_2"), AnalyzedDimension.SOLUTION, "Motivo",
            "Evidencia", evaluation(0.5, 0.7, List.of(), List.of()), "v1");

        assertTrue(opp.explanation().usedInformation().stream()
            .anyMatch(line -> line.contains("50%")));
        assertTrue(opp.explanation().usedInformation().stream()
            .anyMatch(line -> line.contains("Dimensiones cubiertas")));
    }

    @Test
    void build_deberiaSerDeterminista() {
        var a = assembler.build(OpportunityCategory.COMPETITIVA, "Título", "Descripción",
            2, 2, 0, ImpactLevel.HIGH, EffortLevel.MEDIUM,
            List.of("RULE_3"), AnalyzedDimension.COMPETITION, "Motivo",
            "Evidencia", evaluation(0.5, 0.7, List.of(), List.of()), "v1");
        var b = assembler.build(OpportunityCategory.COMPETITIVA, "Título", "Descripción",
            2, 2, 0, ImpactLevel.HIGH, EffortLevel.MEDIUM,
            List.of("RULE_3"), AnalyzedDimension.COMPETITION, "Motivo",
            "Evidencia", evaluation(0.5, 0.7, List.of(), List.of()), "v1");

        assertEquals(a.id(), b.id());
        assertEquals(a.confidence(), b.confidence(), 0.0001);
        assertEquals(a.priority(), b.priority());
    }

    @Test
    void missingBonus_deberiaDistinguirCriticasDeNormales() {
        var eval = evaluation(0.5, 0.7, List.of(AnalyzedDimension.REVENUE_MODEL), List.of());
        assertEquals(3, assembler.missingBonus(eval, AnalyzedDimension.REVENUE_MODEL));
        assertEquals(2, assembler.missingBonus(eval, AnalyzedDimension.MVP));
    }

    @Test
    void hasSignal_deberiaDetectarSeñalesPorPalabraClave() {
        var eval = evaluation(0.5, 0.7, List.of(), List.of("oportunidad de monetización"));
        assertTrue(assembler.hasSignal(eval, "monetiz"));
        assertFalse(assembler.hasSignal(eval, "escal"));
    }

    @Test
    void computePriorityFromScore_deberiaSerInversaAlScore() {
        assertEquals(5, assembler.computePriorityFromScore(0));
        assertEquals(3, assembler.computePriorityFromScore(50));
        assertEquals(1, assembler.computePriorityFromScore(85));
        assertEquals(0, assembler.computePriorityFromScore(95));
        assertEquals(0, assembler.computePriorityFromScore(100));
    }
}
