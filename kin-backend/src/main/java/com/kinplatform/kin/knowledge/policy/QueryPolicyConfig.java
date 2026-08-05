package com.kinplatform.kin.knowledge.policy;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Configuración de políticas de consulta (especificación Fase 2): temas
 * estables que no requieren conocimiento externo y preferencia de caché con su
 * ventana de frescura.
 *
 * <p>Inmutable y determinista: se inyecta por constructor (datos, no lógica) y
 * puede variar por entorno (dev/prod/testing/enterprise) sin tocar el código.</p>
 */
public record QueryPolicyConfig(
    Set<String> stableTopics,
    boolean cacheFirst,
    Duration cacheTtl
) {

    public static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);

    public QueryPolicyConfig {
        stableTopics = lowerCaseSet(stableTopics);
        cacheTtl = cacheTtl == null ? DEFAULT_CACHE_TTL : cacheTtl;
    }

    public static QueryPolicyConfig defaults() {
        return new QueryPolicyConfig(Set.of("scrum", "kanban", "metodologia agil"), true,
            DEFAULT_CACHE_TTL);
    }

    public static QueryPolicyConfig dev() {
        return new QueryPolicyConfig(Set.of(), true, DEFAULT_CACHE_TTL);
    }

    public static QueryPolicyConfig production() {
        return new QueryPolicyConfig(Set.of("scrum", "kanban", "metodologia agil"), true,
            DEFAULT_CACHE_TTL);
    }

    public static QueryPolicyConfig testing() {
        return new QueryPolicyConfig(Set.of("scrum", "kanban", "metodologia agil"), false,
            Duration.ofMinutes(1));
    }

    public static QueryPolicyConfig enterprise() {
        return new QueryPolicyConfig(Set.of("scrum", "kanban", "metodologia agil"), true,
            Duration.ofHours(6));
    }

    private static Set<String> lowerCaseSet(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        var out = new LinkedHashSet<String>();
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(out);
    }
}
