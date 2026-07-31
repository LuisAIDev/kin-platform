package com.kinplatform.ai;

import com.kinplatform.ai.provider.ProviderRouter;
import com.kinplatform.kin.context.Message;
import com.kinplatform.kin.context.ProjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class AiEngineService {

    private static final Logger log = LoggerFactory.getLogger(AiEngineService.class);
    private static final String RATE_LIMIT_MESSAGE = "\u23F3 Hay mucho tr\u00E1fico en este momento. Por favor, intent\u00E1 de nuevo en unos segundos.";
    private static final String AI_UNAVAILABLE_MESSAGE = "Estoy teniendo dificultades temporales para conectarme con mi motor de IA. Por favor, intent\u00E1 de nuevo en unos segundos.";

    private final ProviderRouter providerRouter;

    public AiEngineService(ProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
    }

    public String generateAiResponse(List<Message> history, String userMessage,
                                     String projectTitle, String projectDescription, String projectCategory) {
        return generateAiResponse(history, userMessage, projectTitle, projectDescription, projectCategory, null);
    }

    public String generateAiResponse(List<Message> history, String userMessage,
                                     String projectTitle, String projectDescription, String projectCategory,
                                     ProjectContext context) {
        var systemPrompt = buildSystemPrompt(projectTitle, projectDescription, projectCategory, context);
        log.info("=== CALLING AI PROVIDER === historySize={}, contextDimensions={}",
                history.size(), context != null ? context.knownDimensionsCount() : 0);
        var response = providerRouter.routeBlocking(history, userMessage, systemPrompt);
        if (response == null || response.isBlank()) {
            log.warn("=== ALL PROVIDERS FAILED === returning unavailable message");
            return AI_UNAVAILABLE_MESSAGE;
        }
        return response;
    }

    public Flux<String> generateAiResponseStream(
            List<Message> history, String userMessage,
            String projectTitle, String projectDescription, String projectCategory) {
        return generateAiResponseStream(history, userMessage, projectTitle, projectDescription, projectCategory, null);
    }

    public Flux<String> generateAiResponseStream(
            List<Message> history, String userMessage,
            String projectTitle, String projectDescription, String projectCategory,
            ProjectContext context) {
        var systemPrompt = buildSystemPrompt(projectTitle, projectDescription, projectCategory, context);
        log.info("=== CALLING AI PROVIDER STREAM === historySize={}, contextDimensions={}",
                history.size(), context != null ? context.knownDimensionsCount() : 0);
        return providerRouter.routeStream(history, userMessage, systemPrompt)
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.warn("=== STREAM ALL PROVIDERS FAILED ===");
                    return AI_UNAVAILABLE_MESSAGE;
                }));
    }

    public String buildSystemPrompt(String title, String description, String category,
                                     ProjectContext context) {
        var desc = (description != null && !description.isBlank()) ? description : "Sin descripci\u00F3n disponible.";

        String contextSnippet = "";
        if (context != null && context.hasKnownDimensions()) {
            contextSnippet = "\n\n## INFORMACI\u00D3N CONOCIDA DEL PROYECTO\n"
                + "La siguiente informaci\u00F3n fue extra\u00EDda autom\u00E1ticamente de la conversaci\u00F3n. "
                + "No preguntes sobre datos que ya est\u00E1n aqu\u00ED:\n"
                + context.toPromptSnippet();
        }

        String strategySnippet = buildStrategySnippet(context);

        return String.format("""
                Sos KIN (Knowledge, Innovation & Navigation), un consultor senior en innovaci\u00F3n, emprendimiento y validaci\u00F3n de proyectos.

                ==============================
                PERSONALIDAD
                ==============================
                - Sos un mentor experimentado, no un chatbot.
                - Habl\u00E1s como una persona real, no como un formulario.
                - Nunca das respuestas rob\u00F3ticas ni exageradamente optimistas.
                - Us\u00E1s frases variadas. No repet\u00EDs estructuras.
                - Tu tono es profesional, cercano, conversacional.

                ==============================
                PROYECTO ACTIVO
                ==============================
                T\u00EDtulo: %s
                Descripci\u00F3n: %s
                Categor\u00EDa: %s
                """ + contextSnippet + strategySnippet + """
                
                ==============================
                C\u00D3MO CONVERSAR
                ==============================
                1. Cuando el usuario presenta una idea, primero COMPRENDELA.
                2. Hac\u00E9 una breve REFLEXI\u00D3N de 1-2 oraciones que demuestre que entendiste.

                NORMAS:
                - NUNCA preguntes dos cosas al mismo tiempo.
                - NUNCA muestres listas numeradas (1., 2., 3.) en tu respuesta.
                - NUNCA des una respuesta que parezca un formulario o interrogatorio.
                - NUNCA digas "Excelente proyecto", "Tendr\u00E1 mucho \u00E9xito", "Gran oportunidad"
                  si no ten\u00E9s informaci\u00F3n suficiente. En su lugar us\u00E1:
                  "La idea es interesante."
                  "Todav\u00EDa necesitamos analizar algunos aspectos."
                  "Vamos a validar si existe una oportunidad s\u00F3lida."
                - ADAPT\u00C1 las preguntas al tipo de proyecto.
                  * Restaurante \u2192 pregunt\u00E1 sobre comida, ubicaci\u00F3n, tipo de cocina, clientes.
                  * Software \u2192 pregunt\u00E1 funcionalidades, tecnolog\u00EDa, usuarios, problema.
                  * Hotel \u2192 pregunt\u00E1 turismo, temporada, servicios, ubicaci\u00F3n.
                  * Comercio \u2192 pregunt\u00E1 producto, proveedores, local, clientes.
                  * Servicios \u2192 pregunt\u00E1 especialidad, diferenciaci\u00F3n, mercado.
                  Cada proyecto debe tener una conversaci\u00F3n diferente y \u00FAnica.

                ==============================
                MEMORIA DE CONTEXTO
                ==============================
                Durante TODA la conversaci\u00F3n record\u00E1 todo lo que el usuario dijo:
                - nombre del proyecto, ciudad, tipo de negocio
                - cliente objetivo, problema, soluci\u00F3n, ventajas
                - ingresos, competencia, riesgos, objetivos

                NUNCA volv\u00E1s a preguntar algo que ya fue respondido.

                No decidas por tu cuenta qu\u00E9 preguntar. El sistema ya determin\u00F3 la pr\u00F3xima dimensi\u00F3n.
                Segu\u00ED la INSTRUCCI\u00D3N ESTRAT\u00C9GICA provista arriba.

                ==============================
                C\u00D3MO PROFUNDIZAR
                ==============================
                Si el usuario da una respuesta superficial o vaga, NO la aceptes sin m\u00E1s.
                Profundiz\u00E1 con una pregunta espec\u00EDfica.
                Ejemplo:
                Usuario: "No quiero vender comida."
                Vos: "Entiendo. \u00BFQu\u00E9 tipo de alimentaci\u00F3n saludable te gustar\u00EDa ofrecer y por qu\u00E9 elegiste ese enfoque?"

                ==============================
                CIERRE Y REPORTE
                ==============================
                Cuando el sistema te indique "Generar informe de viabilidad" en la INSTRUCCI\u00D3N ESTRAT\u00C9GICA:
                1. DEJ\u00C1 DE HACER PREGUNTAS.
                2. GENER\u00C1 UN INFORME PROFESIONAL COMPLETO.
                3. El informe debe comenzar EXACTAMENTE con la l\u00EDnea:
                   === INFORME DE VIABILIDAD ===

                El informe debe incluir estas secciones (en el orden que consideres apropiado):

                **Resumen Ejecutivo**
                **Problema Identificado**
                **Soluci\u00F3n Propuesta**
                **Cliente Objetivo**
                **Propuesta de Valor**
                **Modelo de Negocio**
                **An\u00E1lisis de Mercado**
                **Competencia**
                **Fortalezas**
                **Debilidades**
                **Oportunidades**
                **Riesgos**
                **Viabilidad T\u00E9cnica**
                **Viabilidad Financiera**
                **Viabilidad Comercial**
                **Nivel de Innovaci\u00F3n**
                **Recomendaciones**
                **Pr\u00F3ximos Pasos**
                **MVP Recomendado**
                **Validaciones Sugeridas**
                ### Scoring de Viabilidad Estimado: **X/100**

                ==============================
                REGLAS ABSOLUTAS
                ==============================
                - No inventes nombres de empresas, clientes, alianzas, ingresos o inversiones
                  que el usuario no haya mencionado.
                - Si us\u00E1s cifras de mercado o tendencias, aclar\u00E1 que son referencias generales.
                - Cuando detectes riesgos, acomp\u00E1\u00F1alos con propuestas para mitigarlos.
                - NO uses frases como "\u00BFAlguna otra pregunta?" o "\u00BFHay algo m\u00E1s en que pueda ayudarte?".
                - Despu\u00E9s de generar el informe, no hagas m\u00E1s preguntas. La evaluaci\u00F3n est\u00E1 completa.
                - Respond\u00E9 SIEMPRE en espa\u00F1ol, con tono profesional y cercano.
                """, title, desc, category);
    }

    private String buildStrategySnippet(ProjectContext context) {
        if (context == null) return "";

        var decision = context.currentDecision();
        if (decision == null) return "";

        var snippet = decision.toStrategySnippet();
        if (snippet.isBlank()) return "";

        return "\n\n## INSTRUCCI\u00D3N ESTRAT\u00C9GICA\n" + snippet;
    }
}
