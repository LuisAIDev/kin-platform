package com.kinplatform.kin.context;

public interface ContextAnalyzerPort {

    AnalysisResult analyze(String userMessage, ProjectContext currentContext);
}
