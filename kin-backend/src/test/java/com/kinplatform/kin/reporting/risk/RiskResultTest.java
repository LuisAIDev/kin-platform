package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskResultTest {

    private Risk risk(String title) {
        return Risk.create(RiskCategory.BUSINESS, title, "desc",
            RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.HIGH, 0.8,
            RiskExplanation.of(List.of("dato"), "regla", "motivo", "evidencia"),
            List.of("R1"), AnalyzedDimension.PROBLEM, "v1");
    }

    @Test
    void resultado_deberiaSerInmutable() {
        var risks = new ArrayList<>(List.of(risk("A")));
        var top = new ArrayList<>(List.of(risk("A")));
        var result = new RiskResult(risks, RiskLevel.HIGH, top, 0.8, "exp", "RiskEngine", "v1");

        risks.clear();
        top.clear();
        assertEquals(1, result.risks().size());
        assertEquals(1, result.topRisks().size());
        assertThrows(UnsupportedOperationException.class, () -> result.risks().add(risk("B")));
        assertThrows(UnsupportedOperationException.class, () -> result.topRisks().add(risk("B")));
    }

    @Test
    void resultado_deberiaAceptarListasNulas() {
        var result = new RiskResult(null, RiskLevel.LOW, null, 0.5, "x", "e", "v1");
        assertTrue(result.risks().isEmpty());
        assertTrue(result.topRisks().isEmpty());
    }

    @Test
    void resultado_deberiaAcotarConfianza() {
        var result = new RiskResult(List.of(), RiskLevel.LOW, List.of(), 2.0, "x", "e", "v1");
        assertEquals(1.0, result.confidence());
        var result2 = new RiskResult(List.of(), RiskLevel.LOW, List.of(), -1.0, "x", "e", "v1");
        assertEquals(0.0, result2.confidence());
    }

    @Test
    void vacio_deberiaSerSinRiesgos() {
        var result = RiskResult.empty();
        assertFalse(result.hasRisks());
        assertEquals(0, result.riskCount());
        assertEquals(RiskLevel.LOW, result.overallRiskLevel());
        assertEquals(RiskLevel.LOW, result.highestRiskLevel());
    }

    @Test
    void highestRiskLevel_deberiaCalcularMaximaSeveridad() {
        var result = new RiskResult(
            List.of(risk("baja"), risk("critica")), RiskLevel.LOW, List.of(), 0.5, "x", "e", "v1");
        // Ambas son HIGH por el risk() helper
        assertEquals(RiskLevel.HIGH, result.highestRiskLevel());
    }

    @Test
    void risk_deberiaCalcularSeverityScore() {
        var r = Risk.create(RiskCategory.FINANCIAL, "t", "d",
            RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.HIGH, 0.7,
            RiskExplanation.of(List.of(), "r", "y", "e"), List.of("R"), null, "v1");
        assertEquals((2 + 1) * 3 + (1 + 1) * 2 + (2 + 1), r.severityScore());
    }

    @Test
    void risk_deberiaProtegerListasYAcotar() {
        var rules = new ArrayList<>(List.of("R1"));
        var info = new ArrayList<>(List.of("i"));
        var r = new Risk(null, RiskCategory.MARKET, "t", "d",
            RiskLevel.LOW, RiskLevel.LOW, RiskLevel.LOW, 5.0,
            new RiskExplanation(info, "r", "y", "e"), rules, null, null);
        rules.clear();
        info.clear();
        assertEquals(1, r.appliedRules().size());
        assertEquals(1, r.explanation().usedInformation().size());
        assertEquals(1.0, r.confidence());
        assertEquals("", r.engineVersion());
    }

    @Test
    void risk_deberiaAceptarExplicacionYReglasNulas() {
        var r = new Risk(null, RiskCategory.MARKET, "t", "d",
            RiskLevel.LOW, RiskLevel.LOW, RiskLevel.LOW, -1.0,
            null, null, null, null);
        assertNotNull(r.explanation());
        assertTrue(r.explanation().usedInformation().isEmpty());
        assertTrue(r.appliedRules().isEmpty());
        assertEquals(0.0, r.confidence());
        assertEquals("", r.engineVersion());
    }

    @Test
    void create_deberiaGenerarIdDeterminista() {
        var r1 = Risk.create(RiskCategory.TECHNICAL, "T", "D",
            RiskLevel.HIGH, RiskLevel.LOW, RiskLevel.MEDIUM, 0.5,
            RiskExplanation.of(List.of(), "r", "y", "e"), List.of("R"),
            AnalyzedDimension.MVP, "v1");
        var r2 = Risk.create(RiskCategory.TECHNICAL, "T", "D",
            RiskLevel.HIGH, RiskLevel.LOW, RiskLevel.MEDIUM, 0.5,
            RiskExplanation.of(List.of(), "r", "y", "e"), List.of("R"),
            AnalyzedDimension.MVP, "v1");
        assertEquals(r1.id(), r2.id());
    }

    @Test
    void explicacion_deberiaProtegerListaDeInformacion() {
        var info = new ArrayList<>(List.of("dato"));
        var exp = new RiskExplanation(info, "r", "y", "e");
        info.clear();
        assertEquals(1, exp.usedInformation().size());
    }

    @Test
    void explicacion_deberiaAceptarNulos() {
        var exp = new RiskExplanation(null, null, null, null);
        assertTrue(exp.usedInformation().isEmpty());
        assertEquals("", exp.appliedRule());
        assertEquals("", exp.reason());
        assertEquals("", exp.evidence());
    }

    @Test
    void modelo_deberiaTenerValoresPorDefecto() {
        var model = RiskModel.defaultModel();
        assertEquals(40, model.highSeverityCoverageThreshold());
        assertEquals("v1", model.version());
        assertNotNull(model.description());
    }

    @Test
    void categoria_deberiaTenerNombreAmigable() {
        assertEquals("Negocio", RiskCategory.BUSINESS.displayName());
        assertEquals("Técnico", RiskCategory.TECHNICAL.displayName());
        assertEquals("Financiero", RiskCategory.FINANCIAL.displayName());
        assertEquals("Mercado", RiskCategory.MARKET.displayName());
    }
}
