package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.scoring.ScoringEngine;

public class ScoringStage implements PipelineStage {

    private final ScoringEngine scoringEngine;

    public ScoringStage(ScoringEngine scoringEngine) {
        this.scoringEngine = scoringEngine;
    }

    @Override
    public String name() {
        return "Scoring";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return context.projectContext() != null
            && context.evaluation() != null
            && context.decision() != null
            && context.decision().shouldGenerateReport();
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        var score = scoringEngine.evaluate(context.projectContext(), context.evaluation());
        context.scoreResult(score);
        return context;
    }
}
