package com.kinplatform.kin.ai.prompt;

import com.kinplatform.kin.ai.PromptRequest;
import com.kinplatform.kin.ai.PromptType;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConversationPromptBuilderTest {

    private ConversationPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ConversationPromptBuilder();
    }

    private ProjectContext contextConDatos() {
        return ProjectContext.fromProject("Mi App", "App de gestión de tareas", "Software");
    }

    @Test
    void build_deberiaExigirPromptRequestConversation() {
        var exception = assertThrows(IllegalArgumentException.class,
            () -> builder.build(PromptRequest.forReport(com.kinplatform.kin.reporting.report.model.ConsultingReport.empty())));

        assertEquals("ConversationPromptBuilder solo soporta CONVERSATION", exception.getMessage());
    }

    @Test
    void build_deberiaExigirConversationDecisionObligatoria() {
        var exception = assertThrows(IllegalArgumentException.class,
            () -> PromptRequest.forConversation(contextConDatos(), null));

        assertEquals("decision es obligatorio para CONVERSATION", exception.getMessage());
    }

    @Test
    void build_deberiaExigirContextoObligatorio() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar problema");

        var exception = assertThrows(IllegalArgumentException.class,
            () -> PromptRequest.forConversation(null, decision));

        assertEquals("context es obligatorio para CONVERSATION", exception.getMessage());
    }

    @Test
    void build_deberiaIncluirContextoMinimoDelProyecto() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar el problema a resolver");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = builder.build(request);

        assertTrue(prompt.contains("KIN"));
        assertTrue(prompt.contains("Título: Mi App"));
        assertTrue(prompt.contains("Categoría: Software"));
        assertTrue(prompt.contains("Cobertura: 14.3%"));
        assertTrue(prompt.contains("## INSTRUCCIÓN ESTRATÉGICA"));
        assertTrue(prompt.contains("Dimensión prioritaria: Problema que resuelve"));
        assertTrue(prompt.contains("explorar el problema a resolver"));
    }

    @Test
    void build_deberiaIncluirLaInstruccionEstrategicaDeLaDecision() {
        var decision = ConversationDecision.ask(AnalyzedDimension.MVP, 7, "explorar plan de validación");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = builder.build(request);

        assertTrue(prompt.contains("## INSTRUCCIÓN ESTRATÉGICA"));
        assertTrue(prompt.contains("Dimensión prioritaria: MVP / validación temprana"));
        assertTrue(prompt.contains("Prioridad: 7/10"));
        assertTrue(prompt.contains("Hacé UNA SOLA PREGUNTA relevante sobre esta dimensión."));
    }

    @Test
    void build_deberiaIncluirSoloElResumenConocidoDelContexto() {
        var decision = ConversationDecision.ask(AnalyzedDimension.CITY, 5, "ubicación");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = builder.build(request);

        assertTrue(prompt.contains("## INFORMACIÓN CONOCIDA DEL PROYECTO"));
        assertTrue(prompt.contains("- Nombre del proyecto: Mi App"));
        assertTrue(prompt.contains("- Sector / giro del negocio: Software"));
    }

    @Test
    void build_deberiaOmitirSeccionConocida_cuandoNoHayDimensiones() {
        var ctx = ProjectContext.restore(Map.of(), Set.of(), null, 0, false);
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 8, "explorar");
        var request = PromptRequest.forConversation(ctx, decision);

        var prompt = builder.build(request);

        assertFalse(prompt.contains("## INFORMACIÓN CONOCIDA DEL PROYECTO"));
        assertTrue(prompt.contains("Título: Sin título"));
        assertTrue(prompt.contains("Categoría: Sin categoría"));
        assertTrue(prompt.contains("Cobertura: 0.0%"));
    }

    @Test
    void build_noDeberiaContenerNingunaSeccionDeReporte() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = builder.build(request);

        assertFalse(prompt.contains("=== CONSULTING REPORT ==="));
        assertFalse(prompt.contains("--- INSTRUCCIÓN PARA EL LLM ---"));
        assertFalse(prompt.contains("## Resumen Ejecutivo"));
        assertFalse(prompt.contains("## Scoring de Viabilidad"));
        assertFalse(prompt.contains("## Recomendaciones"));
        assertFalse(prompt.contains("## Análisis de Riesgos"));
        assertFalse(prompt.contains("## Oportunidades Identificadas"));
        assertFalse(prompt.contains("## Metadata del Reporte"));
    }

    @Test
    void build_noDeberiaInstruirAlLLMAGenerarElInforme() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = builder.build(request);

        assertFalse(prompt.contains("CIERRE Y REPORTE"));
        assertFalse(prompt.contains("=== INFORME DE VIABILIDAD ==="));
        assertFalse(prompt.contains("GENERE UN INFORME"));
        assertFalse(prompt.contains("Generar el INFORME DE VIABILIDAD completo"));
    }

    @Test
    void build_deberiaUsarSoloCamposPermitidosPorAdr012() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = builder.build(request);

        assertTrue(prompt.contains("Título:"));
        assertTrue(prompt.contains("Categoría:"));
        assertTrue(prompt.contains("Cobertura:"));
        assertTrue(prompt.contains("## INSTRUCCIÓN ESTRATÉGICA"));
        assertFalse(prompt.contains("Project: "));
        assertFalse(prompt.contains("Generated: "));
    }

    @Test
    void promptRequest_conversation_noDeberiaAceptarConsultingReport() {
        var ctx = contextConDatos();
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var report = com.kinplatform.kin.reporting.report.model.ConsultingReport.empty();

        var exception = assertThrows(IllegalArgumentException.class,
            () -> new PromptRequest(report, PromptType.CONVERSATION, ctx, decision));

        assertEquals("consultingReport debe ser null para CONVERSATION", exception.getMessage());
    }
}
