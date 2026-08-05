package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.SourceTrust;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Policy Decision Engine del Knowledge Engine (especificación Fase 2).
 *
 * <p>Motor determinista, POJO puro y sin efectos secundarios: no conoce Spring,
 * HTTP, APIs, proveedores concretos, RAG, vectores ni el LLM. Nunca ejecuta
 * nada; únicamente responde decisiones por categoría (Query, Provider, Quality,
 * Cost y Context) componiendo las reglas registradas (Strategy Pattern) con
 * semántica de especificación (todas deben permitir).</p>
 *
 * <p>Las reglas por defecto se registran con el constructor sin argumentos; las
 * listas son inyectables por constructor para composición o tests (OCP: agregar
 * una regla no modifica el motor). La configuración de reglas se inyecta por
 * llamada como valores inmutables.</p>
 *
 * <p>Las políticas de calidad se componen <em>después</em> de la validación de
 * fuentes (que permanece intacta): nunca la sustituyen.</p>
 */
public class KnowledgePolicyEngine {

    private final List<QueryRule> queryRules;
    private final List<ProviderRule> providerRules;
    private final List<QualityRule> qualityRules;
    private final List<CostRule> costRules;
    private final List<ContextRule> contextRules;

    public KnowledgePolicyEngine() {
        this(defaultQueryRules(), defaultProviderRules(), defaultQualityRules(),
            defaultCostRules(), defaultContextRules());
    }

    /**
     * @param queryRules    reglas de consulta; si es {@code null} se usan las
     *                      reglas por defecto
     * @param providerRules reglas de proveedores; si es {@code null} se usan las
     *                      reglas por defecto
     * @param qualityRules  reglas de calidad; si es {@code null} se usan las
     *                      reglas por defecto
     * @param costRules     reglas de costo; si es {@code null} se usan las reglas
     *                      por defecto
     * @param contextRules  reglas de contexto; si es {@code null} se usan las
     *                      reglas por defecto
     */
    public KnowledgePolicyEngine(List<QueryRule> queryRules, List<ProviderRule> providerRules,
                                 List<QualityRule> qualityRules, List<CostRule> costRules,
                                 List<ContextRule> contextRules) {
        this.queryRules = queryRules == null ? defaultQueryRules() : copy(queryRules);
        this.providerRules = providerRules == null ? defaultProviderRules() : copy(providerRules);
        this.qualityRules = qualityRules == null ? defaultQualityRules() : copy(qualityRules);
        this.costRules = costRules == null ? defaultCostRules() : copy(costRules);
        this.contextRules = contextRules == null ? defaultContextRules() : copy(contextRules);
    }

    /**
     * Decide el modo de adquisición para un request (Query Policies).
     *
     * <p>Determinista: se recorre el primer modo decisivo en orden de registro;
     * si ninguna regla decide, el modo por defecto es consulta externa.</p>
     */
    public QueryPolicyResult decideQuery(KnowledgeRequest request, QueryPolicyConfig config) {
        var cfg = config == null ? QueryPolicyConfig.defaults() : config;
        if (request == null || request.topic() == null || request.topic().isBlank()) {
            return QueryPolicyResult.of(QueryMode.EXTERNAL, "Tema vacío; se consulta externamente");
        }
        for (var rule : queryRules) {
            var result = rule.evaluate(request, cfg);
            if (result != null && result.mode() != QueryMode.EXTERNAL) {
                return result;
            }
        }
        return QueryPolicyResult.of(QueryMode.EXTERNAL,
            "No aplican políticas restrictivas; se consulta externamente");
    }

    /**
     * Selecciona y ordena los tipos de proveedor permitidos (Provider Policies):
     * aplica las reglas de exclusión, ordena por prioridad configurada y acota
     * al límite máximo.
     */
    public ProviderSelection selectProviders(Set<String> candidateTypes, ProviderPolicyConfig config) {
        var cfg = config == null ? ProviderPolicyConfig.defaults() : config;
        var allowed = new ArrayList<String>();
        var rejected = new ArrayList<String>();
        var reasons = new ArrayList<String>();
        if (candidateTypes != null) {
            for (var type : candidateTypes) {
                if (type == null || type.isBlank()) {
                    continue;
                }
                String normalized = type.trim().toLowerCase(Locale.ROOT);
                boolean accepted = true;
                for (var rule : providerRules) {
                    var decision = rule.evaluate(normalized, cfg);
                    if (decision != null && decision.rejected()) {
                        rejected.add(normalized);
                        reasons.addAll(decision.reasons());
                        accepted = false;
                        break;
                    }
                }
                if (accepted) {
                    allowed.add(normalized);
                }
            }
        }
        allowed.sort(Comparator.comparingInt((String type) -> -cfg.priorityOf(type)));
        int limit = cfg.maxProviders();
        if (limit > 0 && allowed.size() > limit) {
            var dropped = new ArrayList<>(allowed.subList(limit, allowed.size()));
            allowed = new ArrayList<>(allowed.subList(0, limit));
            reasons.add("Límite de proveedores (" + limit + ") alcanzado; descartados: "
                + String.join(", ", dropped));
        }
        return ProviderSelection.of(List.copyOf(allowed), List.copyOf(rejected),
            List.copyOf(reasons), limit);
    }

    /**
     * Evalúa la calidad de un candidato (Quality Policies) con la confianza ya
     * derivada por la validación de fuentes. Composición: si alguna regla
     * rechaza, la decisión es rechazo; si ninguna rechaza pero alguna degrada,
     * la decisión es degradación; en otro caso, permitir.
     */
    public PolicyDecision evaluateQuality(KnowledgeCandidate candidate, SourceTrust trust,
                                          QualityPolicyConfig config) {
        var cfg = config == null ? QualityPolicyConfig.defaults() : config;
        var effectiveTrust = trust == null ? SourceTrust.UNVERIFIED : trust;
        var reasons = new ArrayList<String>();
        boolean rejected = false;
        boolean degraded = false;
        String action = "";
        for (var rule : qualityRules) {
            var decision = rule.evaluate(candidate, effectiveTrust, cfg);
            if (decision == null) {
                continue;
            }
            if (decision.rejected()) {
                rejected = true;
                reasons.addAll(decision.reasons());
            } else if (decision.degraded()) {
                degraded = true;
                reasons.addAll(decision.reasons());
                if (action.isBlank()) {
                    action = decision.action();
                }
            }
        }
        if (rejected) {
            return PolicyDecision.reject(PolicyCategory.QUALITY, join(reasons));
        }
        if (degraded) {
            return PolicyDecision.degrade(PolicyCategory.QUALITY, join(reasons), action);
        }
        return PolicyDecision.allow(PolicyCategory.QUALITY);
    }

    /**
     * Verifica si queda presupuesto de recursos (Cost Policies): si alguna regla
     * rechaza, la decisión es rechazo (detener consultas); en otro caso,
     * permitir continuar.
     */
    public PolicyDecision checkBudget(CostBudgetUsage usage, CostPolicyConfig config) {
        var cfg = config == null ? CostPolicyConfig.defaults() : config;
        var effectiveUsage = usage == null ? new CostBudgetUsage(0, 0) : usage;
        var reasons = new ArrayList<String>();
        for (var rule : costRules) {
            var decision = rule.evaluate(effectiveUsage, cfg);
            if (decision != null && decision.rejected()) {
                reasons.addAll(decision.reasons());
            }
        }
        if (!reasons.isEmpty()) {
            return PolicyDecision.reject(PolicyCategory.COST, join(reasons));
        }
        return PolicyDecision.allow(PolicyCategory.COST);
    }

    /**
     * Verifica si el contexto acumulado cabe dentro del presupuesto (Context
     * Policies): si alguna regla rechaza, la decisión es rechazo (acotar el
     * contexto); en otro caso, permitir.
     */
    public PolicyDecision checkContext(ContextBudget budget, ContextPolicyConfig config) {
        var cfg = config == null ? ContextPolicyConfig.defaults() : config;
        var effectiveBudget = budget == null ? new ContextBudget(0, 0, 0) : budget;
        var reasons = new ArrayList<String>();
        for (var rule : contextRules) {
            var decision = rule.evaluate(effectiveBudget, cfg);
            if (decision != null && decision.rejected()) {
                reasons.addAll(decision.reasons());
            }
        }
        if (!reasons.isEmpty()) {
            return PolicyDecision.reject(PolicyCategory.CONTEXT, join(reasons));
        }
        return PolicyDecision.allow(PolicyCategory.CONTEXT);
    }

    private static List<QueryRule> defaultQueryRules() {
        return List.of(new StableTopicQueryRule(), new CacheFirstQueryRule());
    }

    private static List<ProviderRule> defaultProviderRules() {
        return List.of(new ExcludedSourceTypeProviderRule());
    }

    private static List<QualityRule> defaultQualityRules() {
        return List.of(new MinimumTrustQualityRule(), new MaxAgeQualityRule(),
            new AllowedLanguageQualityRule(), new AllowedSourceTypeQualityRule(),
            new AllowedLicenseQualityRule());
    }

    private static List<CostRule> defaultCostRules() {
        return List.of(new QueryBudgetCostRule(), new ExternalCallCostRule());
    }

    private static List<ContextRule> defaultContextRules() {
        return List.of(new FragmentLimitContextRule(), new TokenBudgetContextRule(),
            new ContextSizeLimitContextRule());
    }

    private static <T> List<T> copy(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    private static String join(List<String> reasons) {
        return String.join("; ", reasons);
    }
}
