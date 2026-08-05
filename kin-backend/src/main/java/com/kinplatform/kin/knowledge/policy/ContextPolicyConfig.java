package com.kinplatform.kin.knowledge.policy;

/**
 * Configuración de políticas de contexto (especificación Fase 2): límites del
 * contexto que finalmente podrá consumir la construcción del prompt (máximo de
 * fragmentos, documentos, tokens y tamaño estimado). Un valor {@code 0}
 * significa "sin límite" para esa dimensión.
 *
 * <p>Inmutable y determinista; varía por entorno sin tocar el código.</p>
 */
public record ContextPolicyConfig(
    int maxFragments,
    int maxDocuments,
    int tokenBudget,
    int maxContextSize
) {

    public ContextPolicyConfig {
        maxFragments = Math.max(0, maxFragments);
        maxDocuments = Math.max(0, maxDocuments);
        tokenBudget = Math.max(0, tokenBudget);
        maxContextSize = Math.max(0, maxContextSize);
    }

    public static ContextPolicyConfig defaults() {
        return new ContextPolicyConfig(10, 3, 4000, 0);
    }

    public static ContextPolicyConfig dev() {
        return new ContextPolicyConfig(20, 5, 8000, 0);
    }

    public static ContextPolicyConfig production() {
        return new ContextPolicyConfig(10, 3, 4000, 0);
    }

    public static ContextPolicyConfig testing() {
        return new ContextPolicyConfig(3, 1, 500, 2048);
    }

    public static ContextPolicyConfig enterprise() {
        return new ContextPolicyConfig(50, 10, 16000, 0);
    }
}
