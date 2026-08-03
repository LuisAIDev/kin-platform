package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.BusinessModelInput;
import com.kinplatform.kin.enterprise.engine.result.BusinessModelResult;
import com.kinplatform.kin.enterprise.valueobjects.LeanCanvas;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación determinista del {@link BusinessModelEngine} (Fase 10,
 * Milestone 2D).
 *
 * <p>Construye el Lean Canvas de los nueve bloques a partir del contexto del
 * proyecto y de los resultados del pipeline. Reglas funcionales aplicadas:</p>
 *
 * <ul>
 *   <li>Nunca inventa información: cada bloque se rellena únicamente con datos
 *       reales del contexto; cuando faltan, el bloque queda con
 *       {@code "Por definir"}.</li>
 *   <li>{@code problem} &larr; {@code PROBLEM}, {@code customerSegments}
 *       &larr; {@code TARGET_CUSTOMER}, {@code uniqueValueProposition}
 *       &larr; {@code VALUE_PROPOSITION}, {@code solution}
 *       &larr; {@code SOLUTION} y {@code revenueStreams}
 *       &larr; {@code REVENUE_MODEL}.</li>
 *   <li>{@code channels} y {@code costStructure} se alimentan de los hechos
 *       verificados del {@link KnowledgeResult} cuando existen; si no, quedan
 *       en {@code "Por definir"}.</li>
 *   <li>{@code keyMetrics} se deriva de los títulos de las oportunidades y
 *       recomendaciones prioritarias; {@code unfairAdvantage} de las
 *       oportunidades de innovación/tecnología.</li>
 *   <li>La confianza es la proporción de bloques con datos reales (0.0 si
 *       todos quedan en {@code "Por definir"}).</li>
 * </ul>
 *
 * <p>Motor stateless, thread-safe, sin dependencias, sin Spring y sin efectos
 * secundarios: no se registra en {@code EngineRegistry} (decisión de
 * aislamiento del {@code package-info}).</p>
 */
public class DefaultBusinessModelEngine implements BusinessModelEngine {

    /** Marcador de datos ausentes (regla funcional: nunca inventar). */
    static final String UNDEFINED = "Por definir";

    private static final String ENGINE_NAME = "kin.enterprise:BusinessModel";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String ENGINE_AUTHOR = "KIN Architecture Team";
    private static final int ENGINE_PRIORITY = 80;

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(ENGINE_NAME, ENGINE_VERSION, ENGINE_AUTHOR,
            EnginePhase.EXPLANATION, EngineType.DOMAIN, ENGINE_PRIORITY);
    }

    @Override
    public BusinessModelResult evaluate(BusinessModelInput input) {
        if (input == null || input.context() == null || !input.context().hasKnownDimensions()) {
            return BusinessModelResult.empty();
        }
        var context = input.context();
        var knowledge = input.knowledge();
        var recommendations = input.recommendations();
        var opportunities = input.opportunities();

        List<String> problem = block(context, AnalyzedDimension.PROBLEM);
        List<String> customerSegments = block(context, AnalyzedDimension.TARGET_CUSTOMER);
        List<String> valueProposition = block(context, AnalyzedDimension.VALUE_PROPOSITION);
        List<String> solution = block(context, AnalyzedDimension.SOLUTION);
        List<String> revenueStreams = block(context, AnalyzedDimension.REVENUE_MODEL);
        List<String> channels = channels(knowledge);
        List<String> costStructure = costStructure(knowledge);
        List<String> keyMetrics = keyMetrics(recommendations, opportunities);
        List<String> unfairAdvantage = unfairAdvantage(opportunities);

        var canvas = LeanCanvas.of(
            problem, customerSegments, valueProposition, solution,
            channels, revenueStreams, costStructure, keyMetrics, unfairAdvantage);

        double confidence = confidence(canvas);
        String explanation = buildExplanation(confidence);

        return new BusinessModelResult(canvas, confidence, explanation,
            "BusinessModelEngine", ENGINE_VERSION);
    }

    private List<String> block(ProjectContext context, AnalyzedDimension dimension) {
        String value = context.value(dimension);
        if (value == null || value.isBlank()) {
            return List.of(UNDEFINED);
        }
        return splitText(value);
    }

    private List<String> channels(KnowledgeResult knowledge) {
        var facts = knowledgeFacts(knowledge);
        if (facts.isEmpty()) {
            return List.of(UNDEFINED);
        }
        return facts;
    }

    private List<String> costStructure(KnowledgeResult knowledge) {
        var facts = knowledgeFacts(knowledge);
        if (facts.isEmpty()) {
            return List.of(UNDEFINED);
        }
        return facts;
    }

    private List<String> keyMetrics(RecommendationResult recommendations,
                                    OpportunityResult opportunities) {
        var metrics = new ArrayList<String>();
        if (opportunities != null) {
            for (var opportunity : opportunities.topOpportunities()) {
                if (opportunity.title() != null && !opportunity.title().isBlank()) {
                    metrics.add(opportunity.title());
                }
            }
        }
        if (recommendations != null) {
            for (var recommendation : recommendations.recommendations()) {
                if (recommendation.title() != null && !recommendation.title().isBlank()
                    && metrics.size() < 3) {
                    metrics.add(recommendation.title());
                }
            }
        }
        return metrics.isEmpty() ? List.of(UNDEFINED) : List.copyOf(metrics);
    }

    private List<String> unfairAdvantage(OpportunityResult opportunities) {
        var advantages = new ArrayList<String>();
        if (opportunities != null) {
            for (var opportunity : opportunities.topOpportunities()) {
                var category = opportunity.category();
                if (category != null && (category == com.kinplatform.kin.reporting.opportunity
                        .OpportunityCategory.INNOVACION
                    || category == com.kinplatform.kin.reporting.opportunity
                        .OpportunityCategory.TECNOLOGICA
                    || category == com.kinplatform.kin.reporting.opportunity
                        .OpportunityCategory.COMPETITIVA)
                    && opportunity.title() != null && !opportunity.title().isBlank()) {
                    advantages.add(opportunity.title());
                }
            }
        }
        return advantages.isEmpty() ? List.of(UNDEFINED) : List.copyOf(advantages);
    }

    private List<String> knowledgeFacts(KnowledgeResult knowledge) {
        if (knowledge == null || knowledge.facts().isEmpty()) {
            return List.of();
        }
        var facts = new ArrayList<String>();
        for (var fact : knowledge.facts()) {
            if (fact.claim() != null && !fact.claim().isBlank()) {
                facts.add(fact.claim());
            }
        }
        return List.copyOf(facts);
    }

    private List<String> splitText(String value) {
        var lines = new ArrayList<String>();
        for (String line : value.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                lines.add(trimmed);
            }
        }
        return lines.isEmpty() ? List.of(UNDEFINED) : List.copyOf(lines);
    }

    private double confidence(LeanCanvas canvas) {
        long defined = canvas.problem().stream().filter(this::isDefined).count()
            + canvas.customerSegments().stream().filter(this::isDefined).count()
            + canvas.uniqueValueProposition().stream().filter(this::isDefined).count()
            + canvas.solution().stream().filter(this::isDefined).count()
            + canvas.channels().stream().filter(this::isDefined).count()
            + canvas.revenueStreams().stream().filter(this::isDefined).count()
            + canvas.costStructure().stream().filter(this::isDefined).count()
            + canvas.keyMetrics().stream().filter(this::isDefined).count()
            + canvas.unfairAdvantage().stream().filter(this::isDefined).count();
        return Math.min(1.0, (double) defined / 9.0);
    }

    private boolean isDefined(String value) {
        return value != null && !value.isBlank() && !UNDEFINED.equals(value);
    }

    private String buildExplanation(double confidence) {
        if (confidence == 0.0) {
            return "El Lean Canvas no pudo rellenarse: faltan datos del proyecto.";
        }
        return "Lean Canvas construido con una cobertura de " + Math.round(confidence * 100) + "%.";
    }
}
