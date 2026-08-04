package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.Message;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.risk.RiskResult;

import java.util.List;

/**
 * Construye los prompts narrativos del proyecto empresarial (Fase 10,
 * Milestone 3E): {@code EXECUTIVE_REPORT} y {@code DOFA}.
 *
 * <p>Clase pura del BC Enterprise que serializa los datos deterministas
 * (contexto, Enterprise Score, resultados del pipeline y los documentos de los
 * planes) en el mensaje de usuario y enmarca las instrucciones de generación en
 * el system prompt. La IA únicamente redacta prosa a partir de estos datos: no
 * recibe ninguna otra información y tiene prohibido inventar cifras o hechos
 * (regla rectora: Java decide, el LLM únicamente comunica).</p>
 *
 * <p>La frontera ADR-012 (el prompt REPORT solo consume {@code ConsultingReport})
 * no se toca: este prompt es un canal narrativo propio del BC Enterprise que no
 * pasa por {@code PromptAssembler}.</p>
 */
public final class EnterpriseNarrativePromptBuilder {

    private static final String EXECUTIVE_SYSTEM_PROMPT =
        "Eres el consultor estratégico senior de KIN. Redactá un Executive Report en "
        + "español, profesional y ejecutivo, para el proyecto del cual recibís contexto, "
        + "puntuación, resultados del análisis y documentos de planificación.\n"
        + "Estructurá EXACTAMENTE estas secciones, cada una con su encabezado:\n"
        + "1. Resumen Ejecutivo\n"
        + "2. Análisis Estratégico\n"
        + "3. Viabilidad\n"
        + "4. Hallazgos\n"
        + "5. Oportunidades\n"
        + "6. Riesgos\n"
        + "7. Conclusiones\n"
        + "8. Recomendaciones\n"
        + "Reglas estrictas:\n"
        + "- NO inventes datos, cifras ni hechos: usá EXCLUSIVAMENTE la información "
        + "proporcionada (contexto, Enterprise Score, recomendaciones, oportunidades, "
        + "conocimiento externo, riesgos y documentos).\n"
        + "- Toda afirmación debe poder trazarse a los datos recibidos.\n"
        + "- Redactá en español, con claridad y concisión.";

    private static final String DOFA_SYSTEM_PROMPT =
        "Eres el consultor estratégico senior de KIN. Redactá un análisis DOFA en español "
        + "para el proyecto del cual recibís contexto, puntuación, oportunidades, riesgos "
        + "y conocimiento externo.\n"
        + "Estructurá EXACTAMENTE estos cuadrantes, cada uno con su encabezado y con "
        + "viñetas explicadas:\n"
        + "- Fortalezas\n"
        + "- Debilidades\n"
        + "- Oportunidades\n"
        + "- Amenazas\n"
        + "Reglas estrictas:\n"
        + "- NO inventes hechos: derivá cada punto de los datos proporcionados "
        + "(contexto, Enterprise Score, oportunidades, riesgos y conocimiento externo).\n"
        + "- Redactá en español, con claridad y concisión.";

    private final EnterpriseDocumentAssembler documentAssembler;

    /**
     * @param documentAssembler ensamblador que serializa los resultados de forma
     *                          neutral y determinista (obligatorio)
     */
    public EnterpriseNarrativePromptBuilder(EnterpriseDocumentAssembler documentAssembler) {
        if (documentAssembler == null) {
            throw new IllegalArgumentException("documentAssembler no puede ser null");
        }
        this.documentAssembler = documentAssembler;
    }

    /**
     * Prompt del Executive Report: contexto + score + resultados + documentos.
     *
     * @param context         contexto durable del proyecto (obligatorio)
     * @param recommendations recomendaciones del pipeline (obligatorio)
     * @param opportunities   oportunidades del pipeline (obligatorio)
     * @param knowledge       conocimiento externo verificado (obligatorio)
     * @param riskResult      riesgo del pipeline (obligatorio)
     * @param score           Enterprise Score de la versión (obligatorio)
     * @param documents       documentos deterministas de los planes (obligatorio)
     * @return petición de IA con el system prompt y el mensaje de usuario
     */
    public AIRequest executiveRequest(ProjectContext context,
                                      RecommendationResult recommendations,
                                      OpportunityResult opportunities,
                                      KnowledgeResult knowledge,
                                      RiskResult riskResult,
                                      EnterpriseScore score,
                                      List<DocumentArtifact> documents) {
        var user = new StringBuilder();
        user.append(contextSection(context));
        user.append(scoreSection(score));
        user.append("## RECOMENDACIONES\n");
        user.append(documentAssembler.render(recommendations)).append('\n');
        user.append("## OPORTUNIDADES\n");
        user.append(documentAssembler.render(opportunities)).append('\n');
        user.append("## CONOCIMIENTO EXTERNO\n");
        user.append(documentAssembler.render(knowledge)).append('\n');
        user.append("## RIESGOS\n");
        user.append(documentAssembler.render(riskResult)).append('\n');
        user.append("## DOCUMENTOS DEL PROYECTO\n");
        user.append(documentsSection(documents));
        return new AIRequest(List.<Message>of(), user.toString(), EXECUTIVE_SYSTEM_PROMPT);
    }

    /**
     * Prompt del DOFA: contexto + score + oportunidades + riesgos + conocimiento.
     *
     * @param context       contexto durable del proyecto (obligatorio)
     * @param opportunities oportunidades del pipeline (obligatorio)
     * @param riskResult    riesgo del pipeline (obligatorio)
     * @param knowledge     conocimiento externo verificado (obligatorio)
     * @param score         Enterprise Score de la versión (obligatorio)
     * @return petición de IA con el system prompt y el mensaje de usuario
     */
    public AIRequest dofaRequest(ProjectContext context,
                                 OpportunityResult opportunities,
                                 RiskResult riskResult,
                                 KnowledgeResult knowledge,
                                 EnterpriseScore score) {
        var user = new StringBuilder();
        user.append(contextSection(context));
        user.append(scoreSection(score));
        user.append("## OPORTUNIDADES\n");
        user.append(documentAssembler.render(opportunities)).append('\n');
        user.append("## RIESGOS\n");
        user.append(documentAssembler.render(riskResult)).append('\n');
        user.append("## CONOCIMIENTO EXTERNO\n");
        user.append(documentAssembler.render(knowledge)).append('\n');
        return new AIRequest(List.<Message>of(), user.toString(), DOFA_SYSTEM_PROMPT);
    }

    private String contextSection(ProjectContext context) {
        var sb = new StringBuilder();
        sb.append("## CONTEXTO DEL PROYECTO\n");
        sb.append("Nombre: ").append(value(context, AnalyzedDimension.PROJECT_NAME)).append('\n');
        sb.append("Sector: ").append(value(context, AnalyzedDimension.SECTOR)).append('\n');
        sb.append("Problema: ").append(value(context, AnalyzedDimension.PROBLEM)).append('\n');
        sb.append("Solución: ").append(value(context, AnalyzedDimension.SOLUTION)).append('\n');
        sb.append("Cliente objetivo: ").append(value(context, AnalyzedDimension.TARGET_CUSTOMER)).append('\n');
        sb.append("Propuesta de valor: ").append(value(context, AnalyzedDimension.VALUE_PROPOSITION)).append('\n');
        sb.append("Modelo de ingresos: ").append(value(context, AnalyzedDimension.REVENUE_MODEL)).append('\n');
        sb.append("Competencia: ").append(value(context, AnalyzedDimension.COMPETITION)).append('\n');
        sb.append("Cobertura del contexto: ")
            .append(String.format("%.0f%%", context.coverageRatio() * 100.0)).append('\n');
        return sb.toString();
    }

    private String scoreSection(EnterpriseScore score) {
        var sb = new StringBuilder();
        sb.append("## ENTERPRISE SCORE\n");
        sb.append(documentAssembler.render(score)).append('\n');
        return sb.toString();
    }

    private String documentsSection(List<DocumentArtifact> documents) {
        var sb = new StringBuilder();
        if (documents == null || documents.isEmpty()) {
            sb.append("(sin documentos)\n");
            return sb.toString();
        }
        for (DocumentArtifact document : documents) {
            sb.append("### ").append(document.type()).append('\n');
            sb.append(document.content()).append('\n');
        }
        return sb.toString();
    }

    private String value(ProjectContext context, AnalyzedDimension dimension) {
        String value = context.value(dimension);
        return value == null || value.isBlank() ? "(no definido)" : value;
    }
}
