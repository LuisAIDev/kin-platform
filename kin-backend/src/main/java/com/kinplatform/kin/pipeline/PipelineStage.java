package com.kinplatform.kin.pipeline;

public interface PipelineStage {

    String name();

    boolean supports(PipelineContext context);

    PipelineContext execute(PipelineContext context);
}
