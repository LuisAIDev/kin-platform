package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.InnovationInput;
import com.kinplatform.kin.enterprise.engine.result.InnovationResult;
import com.kinplatform.kin.enterprise.valueobjects.InnovationLevel;
import com.kinplatform.kin.enterprise.valueobjects.InnovationPlan;
import com.kinplatform.kin.reporting.opportunity.Opportunity;
import com.kinplatform.kin.reporting.opportunity.OpportunityCategory;
import com.kinplatform.kin.reporting.opportunity.OpportunityExplanation;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultInnovationEngineTest {

    private final DefaultInnovationEngine engine = new DefaultInnovationEngine();

    @Test
    void metadata_deberiaReportarIdentidadYFase() {
        var metadata = engine.metadata();
        assertEquals("kin.enterprise:Innovation", metadata.name());
        assertEquals(EnginePhase.INNOVATION, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
    }

    @Test
    void evaluate_conInputNull_deberiaRetornarVacio() {
        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void evaluate_conContextoNull_deberiaRetornarVacio() {
        var input = new InnovationInput(null,
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8));
        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_sinOportunidades_deberiaSerIncremental() {
        var input = new InnovationInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.opportunitiesEmpty(),
            EngineTestFixtures.knowledgeEmpty());

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        InnovationPlan plan = result.plan();
        assertEquals(InnovationLevel.INCREMENTAL, plan.innovationLevel());
        assertEquals("Sin definir", plan.defensibility());
        assertEquals(0.0, result.confidence());
        assertTrue(result.explanation().contains("incremental"));
    }

    @Test
    void evaluate_conOportunidades_deberiaDerivarNivelTransformacional() {
        var input = new InnovationInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8));

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        InnovationPlan plan = result.plan();
        assertEquals(InnovationLevel.TRANSFORMATIONAL, plan.innovationLevel());
        assertFalse(plan.differentiators().isEmpty());
        assertTrue(plan.defensibility().contains("Ventaja competitiva"));
        assertFalse(plan.innovationRoadmap().isEmpty());
        assertEquals(0.8, result.confidence());
    }

    @Test
    void evaluate_conTresOportunidadesDeAltaPrioridad_deberiaSerDisruptivo() {
        var opportunities = disruptiveOpportunities();
        var input = new InnovationInput(
            EngineTestFixtures.contextWithAll(),
            opportunities,
            EngineTestFixtures.knowledgeEmpty());

        var result = engine.evaluate(input);

        assertEquals(InnovationLevel.DISRUPTIVE, result.plan().innovationLevel());
    }

    @Test
    void evaluate_conConocimiento_deberiaIncluirRecomendaciones() {
        var input = new InnovationInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.opportunitiesEmpty(),
            EngineTestFixtures.knowledge(0.8));

        var result = engine.evaluate(input);

        assertFalse(result.plan().researchRecommendations().isEmpty());
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        var input = new InnovationInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8));

        assertEquals(engine.evaluate(input), engine.evaluate(input));
    }

    private OpportunityResult disruptiveOpportunities() {
        var opp1 = opportunity(OpportunityCategory.INNOVACION, "Innovación 1", 8);
        var opp2 = opportunity(OpportunityCategory.INNOVACION, "Innovación 2", 8);
        var opp3 = opportunity(OpportunityCategory.TECNOLOGICA, "Tecnología 1", 7);
        return new OpportunityResult(List.of(opp1, opp2, opp3), List.of(opp1, opp2, opp3),
            0.9, "Oportunidades disruptivas.", "OpportunityEngine", "1.0.0");
    }

    private Opportunity opportunity(OpportunityCategory category, String title, int priority) {
        return Opportunity.create(category, title, "Descripción", priority,
            com.kinplatform.kin.reporting.ImpactLevel.HIGH,
            com.kinplatform.kin.reporting.EffortLevel.MEDIUM, 0.9,
            OpportunityExplanation.of(List.of(), "regla", "razón", "evidencia"),
            List.of("r1"), com.kinplatform.kin.context.AnalyzedDimension.SCALABILITY, "1.0.0");
    }
}
