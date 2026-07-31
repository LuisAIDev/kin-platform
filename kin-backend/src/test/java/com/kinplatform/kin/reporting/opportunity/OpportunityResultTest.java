package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpportunityResultTest {

    private Opportunity opportunity(String title) {
        return Opportunity.create(OpportunityCategory.MERCADO, title, "desc",
            7, ImpactLevel.HIGH, EffortLevel.MEDIUM, 0.8,
            OpportunityExplanation.of(List.of("dato"), "regla", "motivo", "evidencia"),
            List.of("R1"), AnalyzedDimension.PROBLEM, "v1");
    }

    @Test
    void resultado_deberiaSerInmutable() {
        var opportunities = new ArrayList<>(List.of(opportunity("A")));
        var top = new ArrayList<>(List.of(opportunity("A")));
        var result = new OpportunityResult(opportunities, top, 0.8, "exp", "OpportunityEngine", "v1");

        opportunities.clear();
        top.clear();
        assertEquals(1, result.opportunities().size());
        assertEquals(1, result.topOpportunities().size());
        assertThrows(UnsupportedOperationException.class, () -> result.opportunities().add(opportunity("B")));
        assertThrows(UnsupportedOperationException.class, () -> result.topOpportunities().add(opportunity("B")));
    }

    @Test
    void resultado_deberiaAceptarListasNulas() {
        var result = new OpportunityResult(null, null, 0.5, "x", "e", "v1");
        assertTrue(result.opportunities().isEmpty());
        assertTrue(result.topOpportunities().isEmpty());
    }

    @Test
    void resultado_deberiaAcotarConfianza() {
        var result = new OpportunityResult(List.of(), List.of(), 2.0, "x", "e", "v1");
        assertEquals(1.0, result.confidence());
        var result2 = new OpportunityResult(List.of(), List.of(), -1.0, "x", "e", "v1");
        assertEquals(0.0, result2.confidence());
    }

    @Test
    void vacio_deberiaSerSinOportunidades() {
        var result = OpportunityResult.empty();
        assertFalse(result.hasOpportunities());
        assertEquals(0, result.opportunityCount());
        assertEquals(0, result.highestPriority());
        assertTrue(result.isEmpty());
    }

    @Test
    void highestPriority_deberiaCalcularMaximaPrioridad() {
        var result = new OpportunityResult(
            List.of(opportunity("baja"), opportunity("alta")), List.of(), 0.5, "x", "e", "v1");
        assertEquals(7, result.highestPriority());
    }

    @Test
    void opportunity_deberiaAcotarPrioridad() {
        var r = Opportunity.create(OpportunityCategory.INNOVACION, "t", "d",
            99, ImpactLevel.HIGH, EffortLevel.HIGH, 0.7,
            OpportunityExplanation.of(List.of(), "r", "y", "e"), List.of("R"), null, "v1");
        assertEquals(10, r.priority());
        var r2 = Opportunity.create(OpportunityCategory.INNOVACION, "t", "d",
            -5, ImpactLevel.HIGH, EffortLevel.HIGH, 0.7,
            OpportunityExplanation.of(List.of(), "r", "y", "e"), List.of("R"), null, "v1");
        assertEquals(1, r2.priority());
    }

    @Test
    void opportunity_deberiaProtegerListasYAcotar() {
        var rules = new ArrayList<>(List.of("R1"));
        var info = new ArrayList<>(List.of("i"));
        var r = new Opportunity(null, OpportunityCategory.MERCADO, "t", "d",
            7, ImpactLevel.HIGH, EffortLevel.MEDIUM, 5.0,
            new OpportunityExplanation(info, "r", "y", "e"), rules, null, null);
        rules.clear();
        info.clear();
        assertEquals(1, r.appliedRules().size());
        assertEquals(1, r.explanation().usedInformation().size());
        assertEquals(1.0, r.confidence());
        assertEquals("", r.engineVersion());
    }

    @Test
    void opportunity_deberiaAceptarExplicacionYReglasNulas() {
        var r = new Opportunity(null, OpportunityCategory.MERCADO, "t", "d",
            7, ImpactLevel.HIGH, EffortLevel.MEDIUM, -1.0,
            null, null, null, null);
        assertNotNull(r.explanation());
        assertTrue(r.explanation().usedInformation().isEmpty());
        assertTrue(r.appliedRules().isEmpty());
        assertEquals(0.0, r.confidence());
        assertEquals("", r.engineVersion());
    }

    @Test
    void create_deberiaGenerarIdDeterminista() {
        var r1 = Opportunity.create(OpportunityCategory.TECNOLOGICA, "T", "D",
            8, ImpactLevel.HIGH, EffortLevel.HIGH, 0.5,
            OpportunityExplanation.of(List.of(), "r", "y", "e"), List.of("R"),
            AnalyzedDimension.MVP, "v1");
        var r2 = Opportunity.create(OpportunityCategory.TECNOLOGICA, "T", "D",
            8, ImpactLevel.HIGH, EffortLevel.HIGH, 0.5,
            OpportunityExplanation.of(List.of(), "r", "y", "e"), List.of("R"),
            AnalyzedDimension.MVP, "v1");
        assertEquals(r1.id(), r2.id());
    }

    @Test
    void explicacion_deberiaProtegerListaDeInformacion() {
        var info = new ArrayList<>(List.of("dato"));
        var exp = new OpportunityExplanation(info, "r", "y", "e");
        info.clear();
        assertEquals(1, exp.usedInformation().size());
    }

    @Test
    void explicacion_deberiaAceptarNulos() {
        var exp = new OpportunityExplanation(null, null, null, null);
        assertTrue(exp.usedInformation().isEmpty());
        assertEquals("", exp.appliedRule());
        assertEquals("", exp.reason());
        assertEquals("", exp.evidence());
    }

    @Test
    void modelo_deberiaTenerValoresPorDefecto() {
        var model = OpportunityModel.defaultModel();
        assertEquals(8, model.highPriorityThreshold());
        assertEquals(5, model.mediumPriorityThreshold());
        assertEquals("v1", model.version());
        assertNotNull(model.description());
    }

    @Test
    void categoria_deberiaTenerNombreAmigable() {
        assertEquals("Mercado", OpportunityCategory.MERCADO.displayName());
        assertEquals("Innovación", OpportunityCategory.INNOVACION.displayName());
        assertEquals("Tecnológica", OpportunityCategory.TECNOLOGICA.displayName());
        assertEquals("Financiera", OpportunityCategory.FINANCIERA.displayName());
        assertEquals("Competitiva", OpportunityCategory.COMPETITIVA.displayName());
        assertEquals("Escalabilidad", OpportunityCategory.ESCALABILIDAD.displayName());
        assertEquals("Automatización", OpportunityCategory.AUTOMATIZACION.displayName());
        assertEquals("Monetización", OpportunityCategory.MONETIZACION.displayName());
    }
}
