package com.kinplatform.kin;

import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.pipeline.Pipeline;
import com.kinplatform.kin.pipeline.PipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class KinMethod {

    private static final Logger log = LoggerFactory.getLogger(KinMethod.class);

    private final Pipeline pipeline;
    private final DomainEventBus eventBus;

    public KinMethod(Pipeline pipeline, DomainEventBus eventBus) {
        this.pipeline = pipeline;
        this.eventBus = eventBus;
    }

    public KinMethodResult execute(KinMethodCommand command) {
        log.info("KinMethod executing for project={}, userId={}", command.projectId(), command.userId());

        var ctx = new PipelineContext(
            command.projectId(),
            command.userId(),
            command.userMessage(),
            command.history(),
            command.projectTitle(),
            command.projectDescription(),
            command.projectCategory()
        );

        var result = pipeline.execute(ctx);

        for (var event : result.events()) {
            eventBus.publish(event);
        }

        return new KinMethodResult(
            result.projectContext(),
            result.evaluation(),
            result.decision(),
            result.aiResponse(),
            result.scoreResult(),
            result.events()
        );
    }
}
