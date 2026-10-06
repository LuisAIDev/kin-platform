package com.kinplatform.kin.enterprise.integration;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.projectinfo.StructuredInfoSourceType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista enriquecida de las 14 dimensiones de {@code ProjectContext} mediante
 * un overlay con {@code project_structured_info}.
 *
 * <p>NO muta el contexto original: la información estructurada se superpone
 * según una política de prioridad de fuentes ({@code USER_INPUT} &gt;
 * {@code IMPORTED_DOCUMENT} &gt; {@code CALCULATED} &gt; {@code ESTIMATED}
 * &gt; {@code AI_SUGGESTED}). Un dato confirmado (contexto o {@code USER_INPUT})
 * nunca se sobrescribe silenciosamente: se conserva y se registra un conflicto.
 * La ausencia de dato se representa como {@code PENDING}, nunca como {@code 0}.</p>
 */
public final class ResolvedContext {

    private final Map<AnalyzedDimension, ResolvedValue> values;
    private final List<String> conflicts;

    private ResolvedContext(Map<AnalyzedDimension, ResolvedValue> values, List<String> conflicts) {
        this.values = values;
        this.conflicts = conflicts;
    }

    /**
     * Resuelve el overlay de las 14 dimensiones. {@code context} puede ser
     * {@code null} (sin contexto conversacional): en ese caso la resolución se
     * construye únicamente a partir de la información estructurada.
     */
    public static ResolvedContext resolve(ProjectContext context, List<StructuredDatum> data) {
        Map<AnalyzedDimension, ResolvedValue> resolved = new EnumMap<>(AnalyzedDimension.class);
        List<String> conflicts = new ArrayList<>();

        for (AnalyzedDimension dimension : AnalyzedDimension.values()) {
            resolved.put(dimension, resolveDimension(context, data, dimension, conflicts));
        }
        return new ResolvedContext(Map.copyOf(resolved), List.copyOf(conflicts));
    }

    public ResolvedValue value(AnalyzedDimension dimension) {
        return values.get(dimension);
    }

    public Map<AnalyzedDimension, ResolvedValue> all() {
        return values;
    }

    public List<String> conflicts() {
        return conflicts;
    }

    private static ResolvedValue resolveDimension(
            ProjectContext context, List<StructuredDatum> data, AnalyzedDimension dimension, List<String> conflicts) {
        ResolvedValue contextValue = contextValue(context, dimension);
        ResolvedValue structuredValue = bestStructuredValue(data, dimension);

        if (contextValue != null && structuredValue != null) {
            if (!sameValue(contextValue.value(), structuredValue.value())) {
                conflicts.add("Conflicto en " + dimension.displayName() + ": el contexto "
                        + "confirma \"" + contextValue.value() + "\" y la información "
                        + "estructurada aporta \"" + structuredValue.value() + "\" ("
                        + structuredValue.sourceType() + "). Se conserva el dato confirmado.");
            }
            return contextValue;
        }
        if (contextValue != null) {
            return contextValue;
        }
        return structuredValue != null ? structuredValue : ResolvedValue.pending();
    }

    private static ResolvedValue contextValue(ProjectContext context, AnalyzedDimension dimension) {
        if (context == null) {
            return null;
        }
        String value = context.value(dimension);
        if (value == null || value.isBlank()) {
            return null;
        }
        return ResolvedValue.of(value, StructuredInfoSourceType.USER_INPUT, "Conversación");
    }

    private static ResolvedValue bestStructuredValue(List<StructuredDatum> data, AnalyzedDimension dimension) {
        List<DimensionSourceMapping.DimensionKey> candidates = DimensionSourceMapping.candidateKeys(dimension);
        ResolvedValue best = null;
        int bestPriority = -1;
        for (DimensionSourceMapping.DimensionKey candidate : candidates) {
            for (StructuredDatum datum : data) {
                if (candidate.section().equalsIgnoreCase(datum.section())
                        && candidate.key().equalsIgnoreCase(datum.key())
                        && datum.hasValue()) {
                    int priority = priority(datum.sourceType());
                    if (priority > bestPriority) {
                        bestPriority = priority;
                        best = ResolvedValue.of(datum.value(), datum.sourceType(), origin(datum.sourceType()));
                    }
                }
            }
        }
        return best;
    }

    private static int priority(StructuredInfoSourceType sourceType) {
        int index = DimensionSourceMapping.SOURCE_PRIORITY.indexOf(sourceType);
        return index < 0 ? -1 : DimensionSourceMapping.SOURCE_PRIORITY.size() - index;
    }

    private static String origin(StructuredInfoSourceType sourceType) {
        return switch (sourceType) {
            case USER_INPUT -> "Usuario";
            case IMPORTED_DOCUMENT -> "Documento";
            case CALCULATED -> "Calculado";
            case ESTIMATED -> "Estimado";
            case AI_SUGGESTED -> "IA";
        };
    }

    private static boolean sameValue(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /** Visibilidad para tests del mapeo de claves por dimensión. */
    static Map<String, List<String>> debugCandidates(AnalyzedDimension dimension) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (DimensionSourceMapping.DimensionKey key : DimensionSourceMapping.candidateKeys(dimension)) {
            map.computeIfAbsent(key.section(), k -> new ArrayList<>()).add(key.key());
        }
        return map;
    }
}

