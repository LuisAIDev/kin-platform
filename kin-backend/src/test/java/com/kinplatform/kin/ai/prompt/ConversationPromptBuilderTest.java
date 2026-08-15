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

    /** Normaliza espacios y saltos de línea para comparar texto de prompts. */
    private static String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
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

    // ------------------------------------------------------------------
    // AUTOCONOCIMIENTO (bloque de identidad, capacidades y límites)
    // ------------------------------------------------------------------

    @Test
    void build_deberiaIncluirElBloqueDeAutoconocimiento() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = builder.build(request);

        assertTrue(prompt.contains("QUIÉN SOS Y QUÉ PODÉS HACER"));
        assertTrue(prompt.contains("IDENTIDAD"));
        assertTrue(prompt.contains("QUÉ NO PODÉS HACER (LÍMITES)"));
    }

    @Test
    void build_autoconocimiento_deberiaPresentarIdentidadEnPrimeraPersona() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("Soy KIN, una plataforma inteligente orientada a ayudarte a estructurar y analizar proyectos."));
        assertTrue(prompt.contains("Knowledge, Innovation & Navigation"));
    }

    @Test
    void build_autoconocimiento_noDeberiaAfirmarSuperioridadFrenteAOtrasIA() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertFalse(prompt.contains("soy mejor que ChatGPT"));
        assertFalse(prompt.contains("soy superior a"));
        assertFalse(prompt.contains("soy la única plataforma"));
        assertFalse(prompt.contains("garantizo que tu proyecto será exitoso"));
    }

    @Test
    void build_autoconocimiento_deberiaExpresarLosLimitesDeCapacidad() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("No podés garantizar que un proyecto tendrá éxito"));
        assertTrue(prompt.contains("No reemplazás a contadores, abogados"));
        assertTrue(prompt.contains("Actualmente no tengo esa capacidad."));
    }

    @Test
    void build_autoconocimiento_deberiaDescribirElFlujoConversacional() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("idea → conversación guiada → estructuración de la información"));
        assertTrue(prompt.contains("análisis cuando existen las condiciones necesarias"));
        assertTrue(prompt.contains("apoyo a la toma de decisiones"));
    }

    @Test
    void build_autoconocimiento_noDeberiaAfirmarAccesoExternoNoImplementado() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("No podés hacer estudios de mercado en tiempo real"));
        assertFalse(prompt.contains("consulto Internet en tiempo real"));
        assertFalse(prompt.contains("tengo acceso a datos bancarios"));
    }

    @Test
    void build_deberiaConservarPersonalidadYConversacionExistentes() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("Sos KIN (Knowledge, Innovation & Navigation), un consultor senior"));
        assertTrue(prompt.contains("NUNCA preguntes dos cosas al mismo tiempo"));
        assertTrue(prompt.contains("Respondé SIEMPRE en español"));
    }

    @Test
    void build_autoconocimiento_deberiaMantenerLaReglaDeNoInventarInformacion() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("nunca la inventes ni la completes silenciosamente"));
        assertTrue(prompt.contains("No inventes nombres de empresas, clientes"));
    }

    // ------------------------------------------------------------------
    // AUTOCONOCIMIENTO — RELACIÓN CON LLM / MOTOR DE IA
    // ------------------------------------------------------------------

    @Test
    void build_autoconocimiento_deberiaDistinguirKINDeDeepSeek() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("KIN no es el modelo de lenguaje que utiliza"));
        assertTrue(prompt.contains("DeepSeek es el modelo/proveedor de IA actualmente configurado"));
        assertTrue(prompt.contains("KIN es la plataforma que usa ese motor"));
    }

    @Test
    void build_autoconocimiento_deberiaMencionarDeepSeekComoComponenteDelMotor() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("El LLM es un componente tecnológico del motor de IA de KIN"));
        assertTrue(prompt.contains("la configuración real actualmente implementada (DeepSeek)"));
    }

    @Test
    void build_autoconocimiento_noDeberiaIdentificarKINConOtrosLLM() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("diferencia entre KIN y ChatGPT, Claude, Gemini u otros LLM"));
        assertTrue(prompt.contains("propósito, especialización, flujo de trabajo"));
        assertTrue(prompt.contains("estructuración del proyecto"));
    }

    @Test
    void build_autoconocimiento_noDeberiaAfirmarSuperioridadFrenteALosLLM() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertFalse(prompt.contains("soy mejor que"));
        assertFalse(prompt.contains("soy más inteligente"));
        assertFalse(prompt.contains("soy superior a"));
        assertFalse(prompt.contains("tengo mejor IA"));
    }

    @Test
    void build_autoconocimiento_noDeberiaAfirmarCapacidadesAjenasNiRendimiento() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("No afirmes que esos sistemas \"no pueden\" hacer determinadas cosas"));
        assertTrue(prompt.contains("No hagas comparaciones de rendimiento que no hayan sido medidas"));
        assertTrue(prompt.contains("No inventes capacidades de otros modelos"));
        assertTrue(prompt.contains("respondé con la configuración real actualmente implementada"));
    }

    // ------------------------------------------------------------------
    // AUTOCONOCIMIENTO — REGLA ABSOLUTA DE IDENTIDAD Y ARQUITECTURA
    // ------------------------------------------------------------------

    @Test
    void build_autoconocimiento_deberiaIncluirReglaAbsolutaDeIdentidad() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("REGLA ABSOLUTA DE IDENTIDAD"));
        assertTrue(prompt.contains("Bajo NINGUNA circunstancia te identifiques como \"Claude\", \"ChatGPT\""));
        assertTrue(prompt.contains("un modelo desarrollado por Anthropic"));
        assertTrue(prompt.contains("un modelo desarrollado por OpenAI"));
        assertTrue(prompt.contains("un modelo desarrollado por Google"));
    }

    @Test
    void build_autoconocimiento_deberiaProhibirIdentificarseComoCualquierLLM() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        // Las prohibiciones NO deben convertirse en afirmaciones de identidad.
        assertFalse(prompt.contains("Soy Claude."));
        assertFalse(prompt.contains("Soy ChatGPT."));
        assertFalse(prompt.contains("Soy Gemini."));
        assertFalse(prompt.contains("Soy DeepSeek."));
        assertFalse(prompt.contains("Soy un modelo desarrollado por"));
    }

    @Test
    void build_autoconocimiento_deberiaExplicarLaPreguntaEresUnaIA() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("¿ERES UNA INTELIGENCIA ARTIFICIAL?"));
        assertTrue(prompt.contains("No respondas simplemente \"Sí, soy una inteligencia artificial\""));
        assertTrue(prompt.contains("KIN es una plataforma inteligente que utiliza inteligencia artificial como uno de sus componentes tecnológicos"));
    }

    @Test
    void build_autoconocimiento_deberiaExplicarQuienGeneraLasRespuestas() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("¿QUIÉN GENERA TUS RESPUESTAS?"));
        assertTrue(prompt.contains("Mis respuestas se generan utilizando el modelo de lenguaje integrado en KIN, actualmente DeepSeek"));
        assertTrue(prompt.contains("se produce dentro de la arquitectura de KIN"));
    }

    @Test
    void build_autoconocimiento_deberiaSepararQueAportaKINYQueAportaLaIA() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("QUÉ APORTA KIN Y QUÉ APORTA LA IA"));
        assertTrue(prompt.contains("KIN aporta: arquitectura de la aplicación"));
        assertTrue(prompt.contains("La IA/LLM aporta: comprender el lenguaje natural"));
        assertTrue(prompt.contains("No digas que el LLM \"es KIN\""));
        assertTrue(prompt.contains("ni que DeepSeek \"decide por KIN\""));
    }

    @Test
    void build_autoconocimiento_deberiaEvitarAntropomorfismoTecnico() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("Evitá el antropomorfismo técnico"));
        assertTrue(prompt.contains("preferí \"KIN integra...\""));
        assertFalse(prompt.contains("Yo soy el cerebro"));
        assertFalse(prompt.contains("Yo soy el modelo"));
    }

    @Test
    void build_autoconocimiento_deberiaManejarOtrosLLMYAsistentes() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("Copilot, Llama, Mistral, Grok, Qwen u otro LLM"));
        assertTrue(prompt.contains("No tengo información suficiente para hacer una comparación específica"));
    }

    @Test
    void build_autoconocimiento_deberiaReflejarElObjetivoEmpresarial() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("OBJETIVO PRINCIPAL"));
        assertTrue(prompt.contains("orientada especialmente a resolver problemas y necesidades empresariales"));
        assertTrue(prompt.contains("identificación de riesgos y oportunidades"));
    }

    @Test
    void build_autoconocimiento_noDeberiaPresentarMonetizacionComoRazonPrincipal() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("MONETIZACIÓN"));
        assertTrue(prompt.contains("No presentes \"ganar dinero\" como la razón principal de KIN"));
        assertTrue(prompt.contains("Si el usuario pregunta específicamente \"¿Cómo gana dinero KIN?\""));
        assertFalse(prompt.contains("Mi objetivo es ganar dinero"));
    }

    @Test
    void build_autoconocimiento_deberiaExplicarQueElUsuarioHablaConKIN() {
        var decision = ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "explorar");
        var request = PromptRequest.forConversation(contextConDatos(), decision);

        var prompt = normalize(builder.build(request));

        assertTrue(prompt.contains("Soy KIN, una plataforma inteligente orientada a ayudarte a estructurar y analizar proyectos"));
        assertTrue(prompt.contains("KIN es la plataforma. El LLM es un componente tecnológico. DeepSeek es el modelo/proveedor actualmente configurado"));
    }
}
