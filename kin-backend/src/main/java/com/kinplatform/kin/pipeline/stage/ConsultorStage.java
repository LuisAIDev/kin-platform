package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.ai.AiEngineService;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;

public class ConsultorStage implements PipelineStage {

    private final AiEngineService aiEngineService;

    public ConsultorStage(AiEngineService aiEngineService) {
        this.aiEngineService = aiEngineService;
    }

    @Override
    public String name() {
        return "Consultor";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return true;
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        var response = aiEngineService.generateAiResponse(
            context.history(),
            context.userMessage(),
            context.projectTitle(),
            context.projectDescription(),
            context.projectCategory(),
            context.projectContext()
        );
        context.aiResponse(response);
        return context;
    }
}
