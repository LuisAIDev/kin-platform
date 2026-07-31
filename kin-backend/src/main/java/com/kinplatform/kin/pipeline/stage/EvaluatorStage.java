package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.CompletenessEvaluator;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;

public class EvaluatorStage implements PipelineStage {

    private final CompletenessEvaluator evaluator;

    public EvaluatorStage(CompletenessEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public String name() {
        return "Evaluador";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return context.projectContext() != null;
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        var evaluation = evaluator.evaluate(context.projectContext());
        context.evaluation(evaluation);
        return context;
    }
}
