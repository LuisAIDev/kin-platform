package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.context.ContextAnalyzerPort;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;

public class AnalyzerStage implements PipelineStage {

    private final ContextAnalyzerPort analyzer;

    public AnalyzerStage(ContextAnalyzerPort analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public String name() {
        return "Analizador";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return context.projectContext() != null;
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        var result = analyzer.analyze(context.userMessage(), context.projectContext());
        context.projectContext().update(result);
        return context;
    }
}
