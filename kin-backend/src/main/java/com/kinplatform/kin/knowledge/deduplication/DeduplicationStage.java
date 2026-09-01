package com.kinplatform.kin.knowledge.deduplication;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import java.util.function.Predicate;

/**
 * Etapa de deduplicación del pipeline (ADR-027).
 *
 * <p>Ejecuta el {@link DeduplicationEngine} sobre los hechos obtenidos en
 * {@link KnowledgeStage} y almacena el {@link DeduplicationResult} en el
 * {@link PipelineContext}. Se ejecuta después de {@link KnowledgeStage}
 * (priority 55 vs 50).</p>
 */
public class DeduplicationStage implements PipelineStage {

    private static final String STAGE_NAME = "Deduplication";

    private final DomainEngine<DeduplicationInput, DeduplicationResult> deduplicationEngine;
    private final Predicate<PipelineContext> supportsPredicate;

    public DeduplicationStage(DomainEngine<DeduplicationInput, DeduplicationResult> deduplicationEngine) {
        this(
                deduplicationEngine,
                context -> context.knowledgeResult() != null
                        && !context.knowledgeResult().isEmpty());
    }

    public DeduplicationStage(
            DomainEngine<DeduplicationInput, DeduplicationResult> deduplicationEngine,
            Predicate<PipelineContext> supportsPredicate) {
        this.deduplicationEngine = deduplicationEngine;
        this.supportsPredicate = supportsPredicate;
    }

    @Override
    public String name() {
        return "Deduplication";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return context.knowledgeResult() != null && !context.knowledgeResult().isEmpty();
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        if (context.knowledgeResult() == null || context.knowledgeResult().isEmpty()) {
            return context;
        }

        var input = new DeduplicationInput(
                context.projectId(),
                context.knowledgeResult().facts(),
                DeduplicationPolicy.EXACT_THEN_FUZZY, // TODO: leer de config
                0.85 // fuzzy threshold
                );

        var result = deduplicationEngine.evaluate(input);

        // Almacenar resultado en el contexto
        context.deduplicationResult(result);

        return context;
    }
}
