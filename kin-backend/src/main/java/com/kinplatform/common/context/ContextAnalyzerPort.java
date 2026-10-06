package com.kinplatform.common.context;

public interface ContextAnalyzerPort {

    AnalysisResult analyze(String userMessage, ProjectContext currentContext);
}

