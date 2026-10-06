package com.kinplatform.ai.observability;

import com.kinplatform.common.knowledge.KnowledgeCandidate;
import com.kinplatform.common.knowledge.KnowledgeRequest;
import com.kinplatform.common.knowledge.SourceTrust;
import com.kinplatform.common.knowledge.policy.ContextBudget;
import com.kinplatform.common.knowledge.policy.CostBudgetUsage;
import com.kinplatform.common.knowledge.policy.ContextPolicyConfig;
import com.kinplatform.common.knowledge.policy.CostPolicyConfig;
import com.kinplatform.common.knowledge.policy.KnowledgePolicyEngine;
import com.kinplatform.common.knowledge.policy.PolicyDecision;
import com.kinplatform.common.knowledge.policy.ProviderPolicyConfig;
import com.kinplatform.common.knowledge.policy.ProviderSelection;
import com.kinplatform.common.knowledge.policy.QueryPolicyConfig;
import com.kinplatform.common.knowledge.policy.QueryPolicyResult;
import com.kinplatform.common.knowledge.policy.QualityPolicyConfig;

import java.util.Set;

/**
 * Decorador observador del {@link KnowledgePolicyEngine} (Fase 7 —
 * observabilidad). Mide la latencia por decisión y registra decisiones y
 * presupuesto agotado. No altera las decisiones del dominio.
 */
public class TimedPolicyEngine extends KnowledgePolicyEngine {

    private final KnowledgeMetrics metrics;

    public TimedPolicyEngine(KnowledgeMetrics metrics) {
        this.metrics = metrics == null ? new KnowledgeMetrics(null) : metrics;
    }

    @Override
    public QueryPolicyResult decideQuery(KnowledgeRequest request, QueryPolicyConfig config) {
        long start = System.nanoTime();
        QueryPolicyResult result = super.decideQuery(request, config);
        metrics.stage("policy", TimedQueryPlanner.toMs(start));
        metrics.policyDecision(result.mode().name());
        return result;
    }

    @Override
    public ProviderSelection selectProviders(Set<String> candidateTypes, ProviderPolicyConfig config) {
        long start = System.nanoTime();
        ProviderSelection result = super.selectProviders(candidateTypes, config);
        metrics.stage("policy", TimedQueryPlanner.toMs(start));
        metrics.providersSelected(result.allowedTypes().size());
        return result;
    }

    @Override
    public PolicyDecision evaluateQuality(KnowledgeCandidate candidate, SourceTrust trust,
                                          QualityPolicyConfig config) {
        long start = System.nanoTime();
        PolicyDecision result = super.evaluateQuality(candidate, trust, config);
        metrics.stage("policy", TimedQueryPlanner.toMs(start));
        metrics.policyDecision(result.verdict().name());
        return result;
    }

    @Override
    public PolicyDecision checkBudget(CostBudgetUsage usage, CostPolicyConfig config) {
        long start = System.nanoTime();
        PolicyDecision result = super.checkBudget(usage, config);
        metrics.stage("policy", TimedQueryPlanner.toMs(start));
        if (result.rejected()) {
            metrics.budgetExhausted();
        }
        return result;
    }

    @Override
    public PolicyDecision checkContext(ContextBudget budget, ContextPolicyConfig config) {
        long start = System.nanoTime();
        PolicyDecision result = super.checkContext(budget, config);
        metrics.stage("policy", TimedQueryPlanner.toMs(start));
        return result;
    }
}

