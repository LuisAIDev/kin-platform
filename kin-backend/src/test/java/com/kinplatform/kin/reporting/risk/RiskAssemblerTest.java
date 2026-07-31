package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskAssemblerTest {

    private final RiskAssembler assembler = new RiskAssembler();

    private CompletenessEvaluation evaluation(double coverage, double quality) {
        return new CompletenessEvaluation(
            coverage, List.of(AnalyzedDimension.MVP), List.of(),
            0.7, CompletenessEvaluation.MaturityLevel.DEVELOPING,
            CompletenessEvaluation.ViabilityLevel.MEDIUM, quality,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, 8, AnalyzedDimension.values().length);
    }

    @Test
    void build_deberiaConstruirRiesgoCompleto() {
        var risk = assembler.build(RiskCategory.BUSINESS, "Título", "Descripción",
            RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.HIGH,
            List.of("RULE_1"), AnalyzedDimension.PROBLEM, "Motivo",
            "Evidencia", evaluation(0.5, 0.7), "v1");

        assertEquals(RiskCategory.BUSINESS, risk.category());
        assertEquals("Título", risk.title());
        assertEquals("Descripción", risk.description());
        assertEquals(RiskLevel.HIGH, risk.severity());
        assertEquals(RiskLevel.MEDIUM, risk.probability());
        assertEquals(RiskLevel.HIGH, risk.impact());
        assertEquals(AnalyzedDimension.PROBLEM, risk.relatedDimension());
        assertEquals(List.of("RULE_1"), risk.appliedRules());
        assertEquals("v1", risk.engineVersion());
        assertTrue(risk.explanation().appliedRule().contains("RULE_1"));
        assertTrue(risk.explanation().reason().contains("Motivo"));
        assertTrue(risk.explanation().evidence().contains("Evidencia"));
    }

    @Test
    void build_deberiaCalcularConfianzaConLaFormulaCompartida() {
        var risk = assembler.build(RiskCategory.BUSINESS, "Título", "Descripción",
            RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.HIGH,
            List.of("RULE_1"), AnalyzedDimension.PROBLEM, "Motivo",
            "Evidencia", evaluation(0.5, 0.7), "v1");

        double expected = 0.35 + 0.35 * 0.5 + 0.3 * 0.7;
        assertEquals(expected, risk.confidence(), 0.0001);
    }

    @Test
    void build_deberiaAcotarConfianzaEntreCeroYUno() {
        var full = assembler.build(RiskCategory.BUSINESS, "A", "B",
            RiskLevel.LOW, RiskLevel.LOW, RiskLevel.LOW,
            List.of("R"), AnalyzedDimension.SECTOR, "M", "E",
            evaluation(1.0, 1.0), "v1");
        assertEquals(1.0, full.confidence(), 0.0001);

        var none = assembler.build(RiskCategory.BUSINESS, "A", "B",
            RiskLevel.LOW, RiskLevel.LOW, RiskLevel.LOW,
            List.of("R"), AnalyzedDimension.SECTOR, "M", "E",
            evaluation(0.0, 0.0), "v1");
        assertEquals(0.35, none.confidence(), 0.0001);
    }

    @Test
    void build_deberiaIncluirCoberturaEnLaExplicacion() {
        var risk = assembler.build(RiskCategory.TECHNICAL, "Título", "Descripción",
            RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.MEDIUM,
            List.of("RULE_2"), AnalyzedDimension.SOLUTION, "Motivo",
            "Evidencia", evaluation(0.5, 0.7), "v1");

        assertTrue(risk.explanation().usedInformation().stream()
            .anyMatch(line -> line.contains("50%")));
        assertTrue(risk.explanation().usedInformation().stream()
            .anyMatch(line -> line.contains("Dimensiones cubiertas")));
    }

    @Test
    void build_deberiaSerDeterminista() {
        var a = assembler.build(RiskCategory.MARKET, "Título", "Descripción",
            RiskLevel.HIGH, RiskLevel.HIGH, RiskLevel.HIGH,
            List.of("RULE_3"), AnalyzedDimension.COMPETITION, "Motivo",
            "Evidencia", evaluation(0.5, 0.7), "v1");
        var b = assembler.build(RiskCategory.MARKET, "Título", "Descripción",
            RiskLevel.HIGH, RiskLevel.HIGH, RiskLevel.HIGH,
            List.of("RULE_3"), AnalyzedDimension.COMPETITION, "Motivo",
            "Evidencia", evaluation(0.5, 0.7), "v1");

        assertEquals(a.id(), b.id());
        assertEquals(a.confidence(), b.confidence(), 0.0001);
    }
}
