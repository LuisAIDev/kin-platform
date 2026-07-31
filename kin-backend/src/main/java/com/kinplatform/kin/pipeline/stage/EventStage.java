package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.event.ConversationCompletedEvent;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.event.ReportGeneratedEvent;
import com.kinplatform.kin.event.ScoreCalculatedEvent;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;

public class EventStage implements PipelineStage {

    @Override
    public String name() {
        return "Eventos";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return context.projectContext() != null;
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        var decision = context.decision();
        if (decision != null) {
            switch (decision.action()) {
                case ASK -> context.addEvent(new QuestionGeneratedEvent(
                    context.projectId(),
                    decision.dimension() != null ? decision.dimension().displayName() : "",
                    decision.explanation()
                ));
                case REPORT -> context.addEvent(new ReportGeneratedEvent(
                    context.projectId(), "markdown"
                ));
                default -> {}
            }
        }
        if (context.scoreResult() != null && context.scoreResult().totalScore() > 0) {
            context.addEvent(new ScoreCalculatedEvent(
                context.projectId(),
                context.scoreResult().totalScore(),
                context.scoreResult().viabilityLabel(),
                context.projectContext().knownDimensionsCount()
            ));
        }
        context.addEvent(new ConversationCompletedEvent(
            context.projectId(),
            context.projectContext().exchangeCount(),
            context.projectContext().knownDimensionsCount(),
            decision != null ? decision.action().name() : "UNKNOWN"
        ));
        return context;
    }
}
