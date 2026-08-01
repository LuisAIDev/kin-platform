package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskCategory;
import com.kinplatform.kin.reporting.risk.RiskExplanation;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import com.kinplatform.kin.reporting.report.model.RisksSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RisksSectionFormatterTest {

    private final RisksSectionFormatter formatter = new RisksSectionFormatter();

    private Risk risk(String title, String reason) {
        return Risk.create(RiskCategory.BUSINESS, title, "descripción",
            RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.LOW, 0.7,
            RiskExplanation.of(List.of(), "regla", reason, "evidencia"),
            List.of("R1"), AnalyzedDimension.RISKS, "v1");
    }

    @Test
    void kind_deberiaSerAnalytic() {
        assertEquals(ReportSectionKind.ANALYTIC, formatter.kind());
    }

    @Test
    void format_deberiaIncluirRiesgosConReglasYExplicacion() {
        var risks = List.of(risk("Riesgo A", "necesita atención"));
        var topRisks = List.of(risk("Riesgo A", "necesita atención"));
        var section = new RisksSection(risks, RiskLevel.HIGH, topRisks, 0.75);

        var result = formatter.format(section);

        assertTrue(result.contains("## Análisis de Riesgos"));
        assertTrue(result.contains("**Nivel global:** HIGH"));
        assertTrue(result.contains("**Confianza:** 75.0%"));
        assertTrue(result.contains("### Riesgos Identificados (1)"));
        assertTrue(result.contains("#### 1. Riesgo A"));
        assertTrue(result.contains("**Categoría:** BUSINESS"));
        assertTrue(result.contains("**Severidad:** HIGH (14)"));
        assertTrue(result.contains("**Probabilidad:** MEDIUM"));
        assertTrue(result.contains("**Impacto:** LOW"));
        assertTrue(result.contains("**Confianza:** 70.0%"));
        assertTrue(result.contains("**Descripción:** descripción"));
        assertTrue(result.contains("**Reglas aplicadas:**"));
        assertTrue(result.contains("- R1"));
        assertTrue(result.contains("_necesita atención_"));
        assertTrue(result.contains("### Top Riesgos (prioritarios)"));
        assertTrue(result.contains("- **Riesgo A** (HIGH, MEDIUM)"));
    }

    @Test
    void format_deberiaManejarRiesgo_sinReglasNiExplicacion() {
        var r = Risk.create(RiskCategory.TECHNICAL, "Riesgo T", "desc",
            RiskLevel.LOW, RiskLevel.LOW, RiskLevel.LOW, 0.3,
            RiskExplanation.of(List.of(), "r", "", ""),
            List.of(), AnalyzedDimension.RESOURCES, "v1");

        var result = formatter.format(new RisksSection(List.of(r), RiskLevel.LOW,
            List.of(), 0.3));

        assertTrue(result.contains("#### 1. Riesgo T"));
        assertFalse(result.contains("**Reglas aplicadas:**"));
        assertFalse(result.contains("_"));
    }

    @Test
    void format_noDeberiaIncluirTopRisks_cuandoEstaVacio() {
        var section = new RisksSection(List.of(risk("A", "r")), RiskLevel.HIGH,
            List.of(), 0.5);

        var result = formatter.format(section);

        assertFalse(result.contains("### Top Riesgos (prioritarios)"));
    }

    @Test
    void format_deberiaMostrarMensajeDefault_cuandoNoHayRiesgos() {
        var result = formatter.format(RisksSection.empty());

        assertTrue(result.contains("## Análisis de Riesgos"));
        assertTrue(result.contains("_Sin riesgos identificados._"));
    }
}
