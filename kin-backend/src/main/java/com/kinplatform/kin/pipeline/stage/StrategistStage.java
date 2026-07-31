package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.strategy.ConversationStrategist;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;

public class StrategistStage implements PipelineStage {

    private final ConversationStrategist strategist;

    public StrategistStage(ConversationStrategist strategist) {
        this.strategist = strategist;
    }

    @Override
    public String name() {
        return "Estratega";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return context.projectContext() != null && context.evaluation() != null;
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        var decision = strategist.decide(context.projectContext(), context.evaluation());
        context.projectContext().attachDecision(decision);
        if (decision.shouldGenerateReport()) {
            context.projectContext().markReportGenerated();
        }
        context.decision(decision);
        return context;
    }
}
