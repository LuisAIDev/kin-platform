package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.Opportunity;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskResult;

import java.util.List;

/**
 * Generador de documentos narrativos del proyecto empresarial (Fase 10,
 * Milestone 3E): {@code EXECUTIVE_REPORT} y {@code DOFA} redactados por la IA.
 *
 * <p>Se ejecuta exclusivamente al final de {@code EnterpriseGenerationService},
 * una vez que los ocho motores deterministas produjeron los planes, los
 * documentos y el Enterprise Score. La IA (vía el puerto {@link AIResponder},
 * implementado por {@code AiEngineService} que rutea DeepSeek/OpenAI/Ollama con
 * fallback en español) recibe los datos estructurados y únicamente redacta
 * prosa narrativa: no sustituye ningún motor determinista.</p>
 *
 * <p>Robustez (offline-first): si la IA devuelve una respuesta nula o en blanco,
 * se ensambla un documento determinista a partir de los datos reales para que
 * la generación nunca quede sin los dos documentos narrativos.</p>
 */
public final class EnterpriseNarrativeGenerator {

    private static final String EXECUTIVE_GENERATOR = "ExecutiveReportGenerator";
    private static final String DOFA_GENERATOR = "DofaGenerator";
    private static final String ENGINE_VERSION = "1.0.0";

    private final AIResponder aiResponder;
    private final EnterpriseDocumentAssembler documentAssembler;
    private final EnterpriseNarrativePromptBuilder promptBuilder;

    /**
     * @param aiResponder       puerto de IA existente (obligatorio)
     * @param documentAssembler ensamblador de documentos (obligatorio)
     * @param promptBuilder     constructor de prompts narrativos (obligatorio)
     */
    public EnterpriseNarrativeGenerator(AIResponder aiResponder,
                                        EnterpriseDocumentAssembler documentAssembler,
                                        EnterpriseNarrativePromptBuilder promptBuilder) {
        if (aiResponder == null) {
            throw new IllegalArgumentException("aiResponder no puede ser null");
        }
        if (documentAssembler == null) {
            throw new IllegalArgumentException("documentAssembler no puede ser null");
        }
        if (promptBuilder == null) {
            throw new IllegalArgumentException("promptBuilder no puede ser null");
        }
        this.aiResponder = aiResponder;
        this.documentAssembler = documentAssembler;
        this.promptBuilder = promptBuilder;
    }

    /**
     * Genera los dos documentos narrativos (Executive Report y DOFA) de la
     * versión a partir de los datos deterministas.
     *
     * @param version                versión del proyecto empresarial
     * @param context                contexto durable del proyecto (obligatorio)
     * @param recommendations        recomendaciones del pipeline (obligatorio)
     * @param opportunities          oportunidades del pipeline (obligatorio)
     * @param knowledge              conocimiento externo verificado (obligatorio)
     * @param riskResult             riesgo del pipeline (obligatorio)
     * @param score                  Enterprise Score de la versión (obligatorio)
     * @param deterministicDocuments documentos de los planes (obligatorio)
     * @return lista inmutable con EXECUTIVE_REPORT y DOFA (en ese orden)
     */
    public List<DocumentArtifact> generate(int version,
                                           ProjectContext context,
                                           RecommendationResult recommendations,
                                           OpportunityResult opportunities,
                                           KnowledgeResult knowledge,
                                           RiskResult riskResult,
                                           EnterpriseScore score,
                                           List<DocumentArtifact> deterministicDocuments) {
        DocumentArtifact executive = documentAssembler.narrative(version, DocumentType.EXECUTIVE_REPORT,
            executiveContent(context, recommendations, opportunities, knowledge, riskResult, score,
                deterministicDocuments),
            EXECUTIVE_GENERATOR, ENGINE_VERSION);
        DocumentArtifact dofa = documentAssembler.narrative(version, DocumentType.DOFA,
            dofaContent(context, opportunities, riskResult, knowledge, score),
            DOFA_GENERATOR, ENGINE_VERSION);
        return List.of(executive, dofa);
    }

    private String executiveContent(ProjectContext context,
                                    RecommendationResult recommendations,
                                    OpportunityResult opportunities,
                                    KnowledgeResult knowledge,
                                    RiskResult riskResult,
                                    EnterpriseScore score,
                                    List<DocumentArtifact> deterministicDocuments) {
        AIRequest request = promptBuilder.executiveRequest(context, recommendations,
            opportunities, knowledge, riskResult, score, deterministicDocuments);
        String content = aiResponder.respond(request);
        return isBlank(content)
            ? executiveFallback(context, recommendations, opportunities, riskResult, score,
                deterministicDocuments)
            : content;
    }

    private String dofaContent(ProjectContext context,
                               OpportunityResult opportunities,
                               RiskResult riskResult,
                               KnowledgeResult knowledge,
                               EnterpriseScore score) {
        AIRequest request = promptBuilder.dofaRequest(context, opportunities, riskResult,
            knowledge, score);
        String content = aiResponder.respond(request);
        return isBlank(content) ? dofaFallback(opportunities, riskResult) : content;
    }

    // ------------------------------------------------------------------
    // Fallbacks deterministas (offline-first): datos reales, sin IA
    // ------------------------------------------------------------------

    private String executiveFallback(ProjectContext context,
                                     RecommendationResult recommendations,
                                     OpportunityResult opportunities,
                                     RiskResult riskResult,
                                     EnterpriseScore score,
                                     List<DocumentArtifact> deterministicDocuments) {
        var sb = new StringBuilder();
        sb.append("Resumen Ejecutivo\n");
        sb.append("El proyecto presenta un Enterprise Score de ").append(score.overallScore())
            .append("/100 (grado ").append(score.grade())
            .append(") con una confianza del ")
            .append(String.format("%.0f%%", score.confidence() * 100.0)).append(".\n\n");
        sb.append("Análisis Estratégico\n");
        sb.append("Market: ").append(fmt(score.market())).append("/100 · Innovación: ")
            .append(fmt(score.innovation())).append("/100 · Finanzas: ").append(fmt(score.financial()))
            .append("/100 · Riesgo: ").append(fmt(score.risk())).append("/100 · Escalabilidad: ")
            .append(fmt(score.scalability())).append("/100 · Equipo: ").append(fmt(score.team()))
            .append("/100 · Sostenibilidad: ").append(fmt(score.sustainability())).append("/100.\n\n");
        sb.append("Viabilidad\n");
        sb.append("Puntuación de viabilidad ").append(fmt(score.viability())).append("/100 sobre una ")
            .append("cobertura del contexto del ")
            .append(String.format("%.0f%%", context.coverageRatio() * 100.0)).append(".\n\n");
        sb.append("Hallazgos\n");
        sb.append("El proyecto cuenta con ").append(deterministicDocuments.size())
            .append(" documento(s) de planificación: ");
        sb.append(deterministicDocuments.stream().map(d -> d.type().name())
            .reduce((a, b) -> a + ", " + b).orElse("(ninguno)")).append(".\n\n");
        sb.append("Oportunidades\n");
        appendTitles(sb, opportunities == null ? null : opportunities.topOpportunities());
        sb.append("Riesgos\n");
        appendTitles(sb, riskResult == null ? null : riskResult.risks());
        sb.append("Conclusiones\n");
        sb.append("Síntesis determinista derivada del Enterprise Score y de los planes "
            + "generados; la generación narrativa por IA no estuvo disponible.\n\n");
        sb.append("Recomendaciones\n");
        appendTitles(sb, recommendations == null ? null : recommendations.recommendations());
        return sb.toString();
    }

    private String dofaFallback(OpportunityResult opportunities, RiskResult riskResult) {
        var sb = new StringBuilder();
        sb.append("Fortalezas\n");
        appendTitles(sb, opportunities == null ? null : opportunities.topOpportunities());
        sb.append("Debilidades\n");
        appendTitles(sb, riskResult == null ? null : riskResult.risks());
        sb.append("Oportunidades\n");
        appendTitles(sb, opportunities == null ? null : opportunities.opportunities());
        sb.append("Amenazas\n");
        appendTitles(sb, riskResult == null ? null : riskResult.risks());
        return sb.toString();
    }

    private static String fmt(double value) {
        return String.format("%.0f", value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void appendTitles(StringBuilder sb, List<?> items) {
        if (items == null || items.isEmpty()) {
            sb.append("- (sin datos)\n");
            return;
        }
        for (Object item : items) {
            String title = titleOf(item);
            if (title != null && !title.isBlank()) {
                sb.append("- ").append(title).append('\n');
            }
        }
        sb.append('\n');
    }

    private static String titleOf(Object item) {
        if (item instanceof Opportunity opportunity) {
            return opportunity.title();
        }
        if (item instanceof Risk risk) {
            return risk.title();
        }
        if (item instanceof Recommendation recommendation) {
            return recommendation.title();
        }
        return null;
    }
}
