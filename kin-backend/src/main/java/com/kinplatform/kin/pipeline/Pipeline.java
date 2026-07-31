package com.kinplatform.kin.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Pipeline {

    private static final Logger log = LoggerFactory.getLogger(Pipeline.class);

    private final List<PipelineStage> stages;

    public Pipeline(List<PipelineStage> stages) {
        this.stages = new ArrayList<>(stages);
    }

    public PipelineContext execute(PipelineContext context) {
        log.info("Pipeline execution started with {} stages", stages.size());
        for (var stage : stages) {
            if (!stage.supports(context)) {
                log.info("Stage '{}' skipped (not supported)", stage.name());
                continue;
            }
            context.currentStage(stage.name());
            log.info("Pipeline stage: '{}' executing...", stage.name());
            long start = System.currentTimeMillis();
            context = stage.execute(context);
            long elapsed = System.currentTimeMillis() - start;
            log.info("Pipeline stage: '{}' completed in {}ms", stage.name(), elapsed);
            if (context.completed()) {
                log.info("Pipeline marked as completed after stage '{}'", stage.name());
                break;
            }
        }
        context.markCompleted();
        log.info("Pipeline execution finished");
        return context;
    }
}
