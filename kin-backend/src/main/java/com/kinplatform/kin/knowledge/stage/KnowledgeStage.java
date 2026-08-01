package com.kinplatform.kin.knowledge.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.knowledge.KnowledgeInput;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.pipeline.stage.EngineStage;

import java.util.ArrayList;
import java.util.List;

/**
 * Etapa de conocimiento del pipeline (ADR-014): ejecuta el {@link KnowledgeEngine}.
 *
 * <p>Composición pura sobre {@link EngineStage} (mismo patrón que
 * {@code ScoringStage}/{@code OpportunityStage}/{@code ReportStage}): lee
 * únicamente el {@link ProjectContext} del {@link PipelineContext}, construye la
 * {@link KnowledgeRequest}, invoca el motor y almacena el {@link KnowledgeResult}
 * en {@code PipelineContext.knowledgeResult} (campo aditivo sancionado por
 * ADR-014).</p>
 *
 * <p>El stage nunca habla con APIs, Internet, HTTP, Spring, el LLM ni ningún
 * adaptador: la adquisición, validación y selección son decisiones deterministas
 * de Java dentro del motor.</p>
 */
public class KnowledgeStage implements PipelineStage {

    private final EngineStage<KnowledgeInput, KnowledgeResult> delegate;

    public KnowledgeStage(KnowledgeEngine knowledgeEngine) {
        this.delegate = new EngineStage<>(
            "Conocimiento",
            knowledgeEngine,
            context -> context != null && context.projectContext() != null,
            context -> new KnowledgeInput(buildRequest(context.projectContext())),
            PipelineContext::knowledgeResult
        );
    }

    private KnowledgeRequest buildRequest(ProjectContext projectContext) {
        return new KnowledgeRequest(
            topic(projectContext),
            projectContext.coveredDimensions(),
            keywords(projectContext),
            KnowledgeRequest.DEFAULT_LIMIT,
            KnowledgeRequest.DEFAULT_TIME_WINDOW);
    }

    private static String topic(ProjectContext projectContext) {
        String topic = projectContext.value(AnalyzedDimension.PROJECT_NAME);
        if (topic == null || topic.isBlank()) {
            topic = projectContext.value(AnalyzedDimension.SOLUTION);
        }
        return topic == null ? "" : topic.strip();
    }

    private static List<String> keywords(ProjectContext projectContext) {
        var keywords = new ArrayList<String>();
        addKeyword(keywords, projectContext.value(AnalyzedDimension.SECTOR));
        addKeyword(keywords, projectContext.value(AnalyzedDimension.PROBLEM));
        addKeyword(keywords, projectContext.value(AnalyzedDimension.TARGET_CUSTOMER));
        return List.copyOf(keywords);
    }

    private static void addKeyword(List<String> keywords, String value) {
        if (value != null && !value.isBlank()) {
            keywords.add(value.strip());
        }
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public boolean supports(PipelineContext context) {
        return delegate.supports(context);
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        return delegate.execute(context);
    }
}
