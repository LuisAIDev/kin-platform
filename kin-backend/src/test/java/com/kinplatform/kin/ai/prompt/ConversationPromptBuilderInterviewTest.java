package com.kinplatform.kin.ai.prompt;

import com.kinplatform.kin.ai.PromptRequest;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.interview.AnswerRules;
import com.kinplatform.kin.interview.InterviewDecision;
import com.kinplatform.kin.interview.InterviewDirective;
import com.kinplatform.kin.interview.InterviewResult;
import com.kinplatform.kin.interview.InterviewState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationPromptBuilderInterviewTest {

    private ConversationPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ConversationPromptBuilder();
    }

    private PromptRequest requestConversacion() {
        var ctx = ProjectContext.fromProject("Mi App", "App de gestión", "Software");
        var decision = ConversationDecision.ask(AnalyzedDimension.SECTOR, 9,
            "Entrevista estratégica: sector del negocio");
        return PromptRequest.forConversation(ctx, decision);
    }

    private InterviewResult resultadoEntrevistaActiva() {
        var directive = InterviewDirective.of("q-sector", AnalyzedDimension.SECTOR,
            "sector y giro del negocio", AnswerRules.of(10, true, 2));
        var state = InterviewState.empty(UUID.randomUUID())
            .withCurrent("q-sector").withPending(List.of("q-sector"));
        return InterviewResult.of(InterviewDecision.ask("q-sector", "Falta información"),
            directive, state, state.toProgress(5));
    }

    @Test
    void build_conDirectivaDeEntrevista_deberiaIncluirLaSeccionEntrevistaEstrategica() {
        var prompt = builder.build(requestConversacion(), resultadoEntrevistaActiva());

        assertTrue(prompt.contains("## ENTREVISTA ESTRATÉGICA"));
        assertTrue(prompt.contains("sector y giro del negocio"));
        assertTrue(prompt.contains("Dimensión: " + AnalyzedDimension.SECTOR.displayName()));
    }

    @Test
    void build_conDirectivaDeEntrevista_deberiaIncluirLasReglasDeValidacion() {
        var prompt = builder.build(requestConversacion(), resultadoEntrevistaActiva());

        assertTrue(prompt.contains("Longitud mínima: 10 caracteres."));
        assertTrue(prompt.contains("Refinamiento permitido: sí (máximo 2)."));
    }

    @Test
    void build_conDirectivaDeEntrevista_deberiaInstruirAFormularSoloLaPregunta() {
        var prompt = builder.build(requestConversacion(), resultadoEntrevistaActiva());

        assertTrue(prompt.contains("Formula SOLO esta pregunta en lenguaje natural"));
        assertTrue(prompt.contains("No inventes preguntas nuevas."));
    }

    @Test
    void build_sinEntrevista_deberiaOmitirLaSeccion() {
        var prompt = builder.build(requestConversacion(), null);

        assertFalse(prompt.contains("## ENTREVISTA ESTRATÉGICA"));
    }

    @Test
    void build_conResultadoDeEntrevistaSinDirectiva_deberiaOmitirLaSeccion() {
        var completa = InterviewResult.of(
            InterviewDecision.report("completa"), null,
            InterviewState.empty(UUID.randomUUID()).withComplete(true), null);

        var prompt = builder.build(requestConversacion(), completa);

        assertFalse(prompt.contains("## ENTREVISTA ESTRATÉGICA"));
    }

    @Test
    void build_conResultadoVacio_deberiaOmitirLaSeccion() {
        var prompt = builder.build(requestConversacion(), InterviewResult.empty());

        assertFalse(prompt.contains("## ENTREVISTA ESTRATÉGICA"));
    }

    @Test
    void build_conEntrevista_deberiaSeguirRechazandoModoReporte() {
        var request = PromptRequest.forReport(
            com.kinplatform.kin.reporting.report.model.ConsultingReport.empty());

        assertThrows(IllegalArgumentException.class,
            () -> builder.build(request, resultadoEntrevistaActiva()));
    }
}
