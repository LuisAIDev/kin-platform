package com.kinplatform.kin.knowledge.policy;

/**
 * Conjunto de políticas por entorno (especificación Fase 2): agrupa las cinco
 * categorías en un único valor inmutable que la capa de aplicación inyecta al
 * dominio. Cambiar reglas = cambiar configuración, sin modificar código.
 */
public record PolicyConfig(
    QueryPolicyConfig query,
    ProviderPolicyConfig provider,
    QualityPolicyConfig quality,
    CostPolicyConfig cost,
    ContextPolicyConfig context
) {

    public PolicyConfig {
        query = query == null ? QueryPolicyConfig.defaults() : query;
        provider = provider == null ? ProviderPolicyConfig.defaults() : provider;
        quality = quality == null ? QualityPolicyConfig.defaults() : quality;
        cost = cost == null ? CostPolicyConfig.defaults() : cost;
        context = context == null ? ContextPolicyConfig.defaults() : context;
    }

    public static PolicyConfig defaults() {
        return new PolicyConfig(QueryPolicyConfig.defaults(), ProviderPolicyConfig.defaults(),
            QualityPolicyConfig.defaults(), CostPolicyConfig.defaults(),
            ContextPolicyConfig.defaults());
    }

    public static PolicyConfig dev() {
        return new PolicyConfig(QueryPolicyConfig.dev(), ProviderPolicyConfig.dev(),
            QualityPolicyConfig.dev(), CostPolicyConfig.dev(), ContextPolicyConfig.dev());
    }

    public static PolicyConfig production() {
        return new PolicyConfig(QueryPolicyConfig.production(), ProviderPolicyConfig.production(),
            QualityPolicyConfig.production(), CostPolicyConfig.production(),
            ContextPolicyConfig.production());
    }

    public static PolicyConfig testing() {
        return new PolicyConfig(QueryPolicyConfig.testing(), ProviderPolicyConfig.testing(),
            QualityPolicyConfig.testing(), CostPolicyConfig.testing(),
            ContextPolicyConfig.testing());
    }

    public static PolicyConfig enterprise() {
        return new PolicyConfig(QueryPolicyConfig.enterprise(), ProviderPolicyConfig.enterprise(),
            QualityPolicyConfig.enterprise(), CostPolicyConfig.enterprise(),
            ContextPolicyConfig.enterprise());
    }
}
