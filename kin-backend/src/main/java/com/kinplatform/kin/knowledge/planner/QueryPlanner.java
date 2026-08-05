package com.kinplatform.kin.knowledge.planner;

import com.kinplatform.kin.knowledge.KnowledgeRequest;

/**
 * Fachada del Query Planner (especificación Fase 3): transforma un
 * {@link KnowledgeRequest} en un {@link QueryPlan} ejecutando el pipeline
 * interno determinista:
 *
 * <pre>KnowledgeRequest → IntentAnalyzer → QueryClassifier → StrategySelector
 *                    → PlanGenerator → QueryPlan</pre>
 *
 * <p>POJO puro de dominio: nunca ejecuta consultas, nunca conoce proveedores
 * concretos, Spring, HTTP, DeepSeek ni infraestructura. El plan solo describe;
 * la ejecución pertenece al orquestador (Fase 5).</p>
 */
public class QueryPlanner {

    private final IntentAnalyzer analyzer;
    private final QueryClassifier classifier;
    private final StrategySelector selector;
    private final PlanGenerator generator;

    public QueryPlanner() {
        this(new IntentAnalyzer(), new QueryClassifier(), new StrategySelector(), new PlanGenerator());
    }

    public QueryPlanner(IntentAnalyzer analyzer, QueryClassifier classifier,
                        StrategySelector selector, PlanGenerator generator) {
        this.analyzer = analyzer == null ? new IntentAnalyzer() : analyzer;
        this.classifier = classifier == null ? new QueryClassifier() : classifier;
        this.selector = selector == null ? new StrategySelector() : selector;
        this.generator = generator == null ? new PlanGenerator() : generator;
    }

    public QueryPlan plan(KnowledgeRequest request) {
        var intent = analyzer.analyze(request);
        var classification = classifier.classify(intent);
        var strategy = selector.select(classification, request);
        return generator.generate(classification, strategy, request);
    }
}
