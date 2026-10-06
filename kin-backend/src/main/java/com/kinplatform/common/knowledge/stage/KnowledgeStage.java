package com.kinplatform.common.knowledge.stage;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.knowledge.KnowledgeInput;
import com.kinplatform.common.knowledge.KnowledgeRequest;
import com.kinplatform.common.knowledge.KnowledgeResult;
import com.kinplatform.common.knowledge.engine.KnowledgeEngine;
import com.kinplatform.common.pipeline.PipelineContext;
import com.kinplatform.common.pipeline.PipelineStage;
import com.kinplatform.common.pipeline.stage.EngineStage;
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
 * <p><strong>Modo sombra</strong> (ADR-025, Fase 1): con {@code shadow=true} se
 * ejecuta el motor completo (red real, validación, caché, métricas
 * {@code kin.knowledge.adapter.*}) pero se **suprime** el resultado del contexto
 * antes de la siguiente etapa; el {@code EnrichmentStage} recibe vacío
 * ({@code EnrichmentEngine} tolera {@code knowledgeResult == null} → resultado
 * vacío) y el usuario no ve ningún cambio. El motor se conserva con un log
 * resumen por turno para observabilidad.</p>
 *
 * <p>El stage nunca habla con APIs, Internet, HTTP, Spring, el LLM ni ningún
 * adaptador: la adquisición, validación y selección son decisiones deterministas
 * de Java dentro del motor.</p>
 */
public class KnowledgeStage implements PipelineStage {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KnowledgeStage.class);

    private final EngineStage<KnowledgeInput, KnowledgeResult> delegate;
    private final boolean shadow;

    public KnowledgeStage(KnowledgeEngine knowledgeEngine) {
        this(knowledgeEngine, false);
    }

    public KnowledgeStage(KnowledgeEngine knowledgeEngine, boolean shadow) {
        this.delegate = new EngineStage<>(
                "Conocimiento",
                knowledgeEngine,
                context -> context != null && context.projectContext() != null,
                context -> new KnowledgeInput(buildRequest(context.projectContext())),
                PipelineContext::knowledgeResult);
        this.shadow = shadow;
    }

    private KnowledgeRequest buildRequest(ProjectContext projectContext) {
        return new KnowledgeRequest(
                topic(projectContext),
                projectContext.coveredDimensions(),
                keywords(projectContext),
                KnowledgeRequest.DEFAULT_LIMIT,
                KnowledgeRequest.DEFAULT_TIME_WINDOW,
                category(projectContext));
    }

    /**
     * Categoría del proyecto (ADR-024): se almacena en un campo dedicado de
     * {@link ProjectContext} al crear el contexto (no la sobrescribe el
     * analizador). Permite que el {@code CategoryAwareCompositeKnowledgeSource}
     * seleccione solo las fuentes pertinentes. Java decide; el LLM nunca elige
     * fuentes.
     */
    private static String category(ProjectContext projectContext) {
        String category = projectContext.projectCategory();
        if (category == null || category.isBlank()) {
            category = projectContext.value(AnalyzedDimension.SECTOR);
        }
        return category == null ? "" : category.strip();
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
        long start = System.nanoTime();
        PipelineContext executed = delegate.execute(context);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        KnowledgeResult result = executed.knowledgeResult();
        if (shadow) {
            // Modo sombra (ADR-025): el motor corrió (métricas/caché) pero no se
            // propaga el resultado → el enriquecimiento recibe vacío (invisible).
            executed.knowledgeResult(null);
            log.info(
                    "[shadow] KnowledgeStage ejecutado: duracionMs={}, facts={}, fuentes={}, sin propagacion",
                    elapsedMs,
                    result == null ? 0 : result.factCount(),
                    result == null ? List.of() : result.sourcesUsed());
        } else {
            log.debug("KnowledgeStage: duracionMs={}, facts={}", elapsedMs, result == null ? 0 : result.factCount());
        }
        return executed;
    }
}



