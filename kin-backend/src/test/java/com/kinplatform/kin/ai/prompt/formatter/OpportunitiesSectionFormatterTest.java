package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;
import com.kinplatform.kin.reporting.opportunity.Opportunity;
import com.kinplatform.kin.reporting.opportunity.OpportunityCategory;
import com.kinplatform.kin.reporting.opportunity.OpportunityExplanation;
import com.kinplatform.kin.reporting.report.model.OpportunitiesSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpportunitiesSectionFormatterTest {

    private final OpportunitiesSectionFormatter formatter = new OpportunitiesSectionFormatter();

    private Opportunity opp(String title, String reason) {
        return Opportunity.create(OpportunityCategory.MERCADO, title, "descripción",
            8, ImpactLevel.HIGH, EffortLevel.MEDIUM, 0.8,
            OpportunityExplanation.of(List.of(), "regla", reason, "evidencia"),
            List.of("O1"), AnalyzedDimension.TARGET_CUSTOMER, "v1");
    }

    @Test
    void kind_deberiaSerAnalytic() {
        assertEquals(ReportSectionKind.ANALYTIC, formatter.kind());
    }

    @Test
    void format_deberiaIncluirOportunidadesConReglasYExplicacion() {
        var opps = List.of(opp("Expansión mercado latino", "fuerte demanda"));
        var topOpps = List.of(opp("Expansión mercado latino", "fuerte demanda"));
        var section = new OpportunitiesSection(opps, topOpps, 0.8);

        var result = formatter.format(section);

        assertTrue(result.contains("## Oportunidades Identificadas"));
        assertTrue(result.contains("**Total:** 1"));
        assertTrue(result.contains("**Confianza:** 80.0%"));
        assertTrue(result.contains("### 1. Expansión mercado latino"));
        assertTrue(result.contains("**Categoría:** MERCADO"));
        assertTrue(result.contains("**Prioridad:** 8/10"));
        assertTrue(result.contains("**Impacto:** HIGH"));
        assertTrue(result.contains("**Esfuerzo:** MEDIUM"));
        assertTrue(result.contains("**Confianza:** 80.0%"));
        assertTrue(result.contains("**Descripción:** descripción"));
        assertTrue(result.contains("**Reglas aplicadas:**"));
        assertTrue(result.contains("- O1"));
        assertTrue(result.contains("_fuerte demanda_"));
        assertTrue(result.contains("### Top Oportunidades (prioritarias)"));
        assertTrue(result.contains("- **Expansión mercado latino** (prioridad 8, impacto HIGH)"));
    }

    @Test
    void format_deberiaManejarOportunidad_sinReglasNiExplicacion() {
        var o = Opportunity.create(OpportunityCategory.MONETIZACION, "Nueva fuente",
            "desc", 5, ImpactLevel.LOW, EffortLevel.LOW, 0.3,
            OpportunityExplanation.of(List.of(), "r", "", ""),
            List.of(), AnalyzedDimension.REVENUE_MODEL, "v1");

        var result = formatter.format(new OpportunitiesSection(List.of(o), List.of(), 0.3));

        assertTrue(result.contains("### 1. Nueva fuente"));
        assertFalse(result.contains("**Reglas aplicadas:**"));
        assertFalse(result.contains("_fuerte demanda"));
    }

    @Test
    void format_noDeberiaIncluirTopOpportunities_cuandoEstaVacio() {
        var section = new OpportunitiesSection(List.of(opp("A", "r")), List.of(), 0.5);

        var result = formatter.format(section);

        assertFalse(result.contains("### Top Oportunidades (prioritarias)"));
    }

    @Test
    void format_deberiaMostrarMensajeDefault_cuandoNoHayOportunidades() {
        var result = formatter.format(OpportunitiesSection.empty());

        assertTrue(result.contains("## Oportunidades Identificadas"));
        assertTrue(result.contains("_Sin oportunidades identificadas._"));
    }
}
