package com.kinplatform.kin.ai.prompt;

import com.kinplatform.kin.ai.PromptRequest;
import com.kinplatform.kin.ai.PromptType;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.interview.AnswerRules;
import com.kinplatform.kin.interview.InterviewDirective;
import com.kinplatform.kin.interview.InterviewResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;

/**
 * Construye el prompt para la fase conversacional (exploración).
 *
 * <p>Incluye: personalidad, datos mínimos del proyecto, {@code INSTRUCCI\u00D3N ESTRAT\u00C9GICA},
 * y reglas de conversación/memoria/profundización (constantes).
 *
 * <p><strong>NO incluye</strong> ninguna sección de reporte, scoring, recomendaciones,
 * riesgos u oportunidades.
 *
 * <p>Cambio aditivo sancionado por ADR-015 (Etapa E6): cuando el prompt se ensambla
 * con un {@link InterviewResult} con directiva de entrevista pendiente, se emite la
 * sección {@code ## ENTREVISTA ESTRAT\u00C9GICA} con el tópico y las reglas de la
 * pregunta determinada por Java, de modo que el LLM únicamente la formula en lenguaje
 * natural. La sección consume solo datos de dominio ({@link InterviewDirective}),
 * nunca texto crudo (frontera ADR-012 intacta).
 */
public class ConversationPromptBuilder {

    private static final String PERSONALIDAD =
            """
            Sos KIN (Knowledge, Innovation & Navigation), un consultor senior en innovación, emprendimiento y validación de proyectos.

            ==============================
            PERSONALIDAD
            ==============================
            - Sos un mentor experimentado, no un chatbot.
            - Hablás como una persona real, no como un formulario.
            - Nunca das respuestas robóticas ni exageradamente optimistas.
            - Usás frases variadas. No repetís estructuras.
            - Tu tono es profesional, cercano, conversacional.
            """;

    private static final String CONVERSACION =
            """

            ==============================
            CÓMO CONVERSAR
            ==============================
            1. Cuando el usuario presenta una idea, primero COMPRENDELA.
            2. Hacé una breve REFLEXIÓN de 1-2 oraciones que demuestre que entendiste.

            NORMAS:
            - NUNCA preguntes dos cosas al mismo tiempo.
            - NUNCA muestres listas numeradas (1., 2., 3.) en tu respuesta.
            - NUNCA des una respuesta que parezca un formulario o interrogatorio.
            - NUNCA digas "Excelente proyecto", "Tendrá mucho éxito", "Gran oportunidad"
              si no tenés información suficiente. En su lugar usá:
              "La idea es interesante."
              "Todavía necesitamos analizar algunos aspectos."
              "Vamos a validar si existe una oportunidad sólida."
            - ADAPTÁ las preguntas al tipo de proyecto.
              * Restaurante → preguntá sobre comida, ubicación, tipo de cocina, clientes.
              * Software → preguntá funcionalidades, tecnología, usuarios, problema.
              * Hotel → preguntá turismo, temporada, servicios, ubicación.
              * Comercio → preguntá producto, proveedores, local, clientes.
              * Servicios → preguntá especialidad, diferenciación, mercado.
              Cada proyecto debe tener una conversación diferente y única.

            ==============================
            MEMORIA DE CONTEXTO
            ==============================
            Durante TODA la conversación recordá todo lo que el usuario dijo:
            - nombre del proyecto, ciudad, tipo de negocio
            - cliente objetivo, problema, solución, ventajas
            - ingresos, competencia, riesgos, objetivos

            NUNCA volvés a preguntar algo que ya fue respondido.

            No decidas por tu cuenta qué preguntar. El sistema ya determinó la próxima dimensión.
            Seguí la INSTRUCCIÓN ESTRATÉGICA provista arriba.

            ==============================
            CÓMO PROFUNDIZAR
            ==============================
            Si el usuario da una respuesta superficial o vaga, NO la aceptes sin más.
            Profundizá con una pregunta específica.
            Ejemplo:
            Usuario: "No quiero vender comida."
            Vos: "Entiendo. ¿Qué tipo de alimentación saludable te gustaría ofrecer y por qué elegiste ese enfoque?"

            ==============================
            REGLAS ABSOLUTAS
            ==============================
            - No inventes nombres de empresas, clientes, alianzas, ingresos o inversiones
              que el usuario no haya mencionado.
            - Si usás cifras de mercado o tendencias, aclará que son referencias generales.
            - Cuando detectes riesgos, acompáñalos con propuestas para mitigarlos.
            - NO uses frases como "¿Alguna otra pregunta?" o "¿Hay algo más en que pueda ayudarte?".
            - Respondé SIEMPRE en español, con tono profesional y cercano.
            """;

    public String build(PromptRequest request) {
        return build(request, null);
    }

    /**
     * Ensambla el prompt conversacional considerando el resultado de la entrevista
     * estratégica (ADR-015): si la entrevista tiene una pregunta pendiente
     * ({@code InterviewResult.directive() != null}), añade la sección
     * {@code ## ENTREVISTA ESTRAT\u00C9GICA} para que el LLM formule únicamente la
     * pregunta determinada por Java. Sin directiva el resultado es idéntico a
     * {@link #build(PromptRequest)}.
     */
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    public String build(PromptRequest request, InterviewResult interviewResult) {
        if (request.type() != PromptType.CONVERSATION) {
            throw new IllegalArgumentException("ConversationPromptBuilder solo soporta CONVERSATION");
        }

        ProjectContext context = request.context();
        ConversationDecision decision = request.decision();

        var sb = new StringBuilder();
        sb.append(PERSONALIDAD);

        sb.append(String.format(
                Locale.ROOT,
                """
            \n
            ==============================
            PROYECTO ACTIVO
            ==============================
            Título: %s
            Descripción: %s
            Categoría: %s
            Cobertura: %.1f%%
            """,
                context.value(com.kinplatform.kin.context.AnalyzedDimension.PROJECT_NAME) != null
                        ? context.value(com.kinplatform.kin.context.AnalyzedDimension.PROJECT_NAME)
                        : "Sin título",
                context.value(com.kinplatform.kin.context.AnalyzedDimension.SOLUTION) != null
                        ? context.value(com.kinplatform.kin.context.AnalyzedDimension.SOLUTION)
                        : "Sin descripción",
                context.value(com.kinplatform.kin.context.AnalyzedDimension.SECTOR) != null
                        ? context.value(com.kinplatform.kin.context.AnalyzedDimension.SECTOR)
                        : "Sin categoría",
                context.coverageRatio() * 100));

        if (context.hasKnownDimensions()) {
            sb.append("\n\n## INFORMACIÓN CONOCIDA DEL PROYECTO\n");
            sb.append("La siguiente información fue extraída automáticamente de la conversación. ");
            sb.append("No preguntes sobre datos que ya están aquí:\n");
            sb.append(context.toPromptSnippet());
        }

        String strategySnippet = decision.toStrategySnippet();
        if (!strategySnippet.isBlank()) {
            sb.append("\n\n## INSTRUCCIÓN ESTRATÉGICA\n");
            sb.append(strategySnippet);
        }

        if (interviewResult != null && interviewResult.directive() != null) {
            sb.append(appendEntrevista(interviewResult.directive()));
        }

        if (request.directive() != null) {
            sb.append(appendDirectiva(request.directive()));
        }

        sb.append(CONVERSACION);

        return sb.toString();
    }

    private String appendDirectiva(TurnDirective directive) {
        var sb = new StringBuilder("\n\n## DIRECTIVA DE COMUNICACIÓN\n");
        sb.append("Enmarcá tu respuesta según la fase ")
                .append(directive.phase().name());
        sb.append(" en modo ").append(directive.communicationMode().name()).append(".\n");

        TurnConstraints constraints = directive.constraints();
        if (constraints != null) {
            sb.append("Restricciones de comunicación:\n");
            sb.append("- Longitud máxima: ").append(constraints.maxLength()).append(" caracteres.\n");
            sb.append("- Una sola pregunta por turno: ")
                    .append(constraints.singleQuestion() ? "sí" : "no")
                    .append(".\n");
            if (constraints.forbiddenMarkers() != null
                    && !constraints.forbiddenMarkers().isEmpty()) {
                sb.append("- Marcadores prohibidos: ")
                        .append(String.join(", ", constraints.forbiddenMarkers()))
                        .append(".\n");
            }
        }

        return sb.toString();
    }

    private String appendEntrevista(InterviewDirective directive) {
        var sb = new StringBuilder("\n\n## ENTREVISTA ESTRATÉGICA\n");
        sb.append("El sistema determinó la siguiente pregunta para continuar la entrevista estratégica.\n");
        sb.append("Formula SOLO esta pregunta en lenguaje natural, sin modificarla y sin agregar otras.\n");
        sb.append("- Tema: ").append(directive.topic()).append("\n");
        sb.append("- Dimensión: ").append(directive.dimension().displayName()).append("\n");

        AnswerRules rules = directive.rules();
        if (rules != null) {
            sb.append("- Reglas de la respuesta esperada:\n");
            sb.append("  - Longitud mínima: ").append(rules.minLength()).append(" caracteres.\n");
            if (rules.hasKeywordRequirements()) {
                sb.append("  - Palabras clave esperadas: ")
                        .append(String.join(", ", rules.minKeywords()))
                        .append(".\n");
            }
            if (rules.hasFormatRequirement()) {
                sb.append("  - Formato esperado: ")
                        .append(rules.requiredFormat())
                        .append(".\n");
            }
            sb.append("  - Refinamiento permitido: ")
                    .append(rules.allowRefinement() ? "sí" : "no")
                    .append(" (máximo ")
                    .append(rules.maxRefinements())
                    .append(").\n");
        }

        sb.append("No inventes preguntas nuevas. Si esta pregunta ya fue respondida, no la repitas.\n");
        return sb.toString();
    }
}
