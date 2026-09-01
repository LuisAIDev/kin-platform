package com.kinplatform.kin.knowledge.deduplication;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Motor de deduplicación de hechos de conocimiento (ADR-027).
 *
 * <p>Recibe una lista de {@link KnowledgeFact} candidatos y aplica una serie de
 * estrategias de deduplicación configurables (exacta, fuzzy, semántica) en orden
 * de prioridad. Produce un {@link DeduplicationResult} con los hechos únicos,
 * los grupos de duplicados detectados y métricas de calidad.</p>
 *
 * <p>Principio: "Java decide. El LLM solo comunica." La deduplicación es
 * determinística y no involucra LLM.</p>
 */
public class DeduplicationEngine implements DomainEngine<DeduplicationInput, DeduplicationResult> {

    public static final String ENGINE_NAME = "DeduplicationEngine";
    public static final String ENGINE_VERSION = "v1";

    private final List<DeduplicationStrategy> strategies;

    public DeduplicationEngine(List<DeduplicationStrategy> strategies) {
        this.strategies = strategies == null ? defaultStrategies() : List.copyOf(strategies);
    }

    private static List<DeduplicationStrategy> defaultStrategies() {
        return List.of(new ExactMatchStrategy(), new FuzzyMatchStrategy(0.85), new SemanticMatchStrategy(0.90));
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(
                ENGINE_NAME,
                ENGINE_VERSION,
                "KIN Architecture Team",
                EnginePhase.KNOWLEDGE,
                EngineType.DOMAIN,
                55 // Priority después de KnowledgeEngine (50)
                );
    }

    @Override
    public DeduplicationResult evaluate(DeduplicationInput input) {
        if (input == null || input.candidates() == null || input.candidates().isEmpty()) {
            return DeduplicationResult.empty();
        }

        // Verificar feature flag
        if (!isEnabled()) {
            return new DeduplicationResult(
                    input.candidates(),
                    Map.of(),
                    new DeduplicationMetrics(
                            input.candidates().size(),
                            input.candidates().size(),
                            0,
                            1.0,
                            List.of(new StrategyMetric(
                                    "DISABLED", input.candidates().size(), 0, 0, 1.0))));
        }

        List<KnowledgeFact> remaining = new ArrayList<>(input.candidates());
        Map<KnowledgeFact, List<KnowledgeFact>> duplicateGroups = new LinkedHashMap<>();
        Map<String, StrategyMetric> metricsByStrategy = new HashMap<>();

        // Aplicar cada estrategia en orden de prioridad
        for (DeduplicationStrategy strategy : strategies) {
            if (!input.policy().getStrategyNames().contains(strategy.strategyName())) {
                continue; // Estrategia no habilitada en la política
            }

            var result = applyStrategy(strategy, remaining, input.fuzzyThreshold());
            remaining = result.remaining();
            duplicateGroups.putAll(result.duplicateGroups());

            metricsByStrategy.put(
                    strategy.strategyName(),
                    new StrategyMetric(
                            strategy.strategyName(),
                            result.inputSize(),
                            result.duplicatesFound(),
                            result.duplicatesRemoved(),
                            result.confidence()));
        }

        // Calcular métricas globales
        int totalInput = input.candidates().size();
        int uniqueCount = remaining.size();
        int totalRemoved = totalInput - uniqueCount;

        double avgConfidence = metricsByStrategy.values().stream()
                .mapToDouble(StrategyMetric::confidence)
                .average()
                .orElse(1.0);

        var metrics = new DeduplicationMetrics(
                input.candidates().size(),
                uniqueCount,
                totalRemoved,
                avgConfidence,
                metricsByStrategy.values().stream().toList());

        return new DeduplicationResult(remaining, Map.copyOf(duplicateGroups), metrics);
    }

    private StrategyResult applyStrategy(
            DeduplicationStrategy strategy, List<KnowledgeFact> candidates, double fuzzyThreshold) {
        List<KnowledgeFact> remaining = new ArrayList<>(candidates);
        Map<KnowledgeFact, List<KnowledgeFact>> groups = new LinkedHashMap<>();
        int duplicatesFound = 0;
        int duplicatesRemoved = 0;

        // Agrupar por sourceId + category para reducir comparaciones
        Map<String, List<KnowledgeFact>> bySourceAndCategory =
                candidates.stream().collect(Collectors.groupingBy(f -> f.sourceId() + "|" + f.category()));

        for (List<KnowledgeFact> group : bySourceAndCategory.values()) {
            if (group.size() <= 1) {
                continue;
            }

            // Comparar pares dentro del grupo
            for (int i = 0; i < group.size(); i++) {
                KnowledgeFact a = group.get(i);
                if (!remaining.contains(a)) {
                    continue; // Ya fue removido
                }

                for (int j = i + 1; j < group.size(); j++) {
                    KnowledgeFact b = group.get(j);
                    if (!remaining.contains(b)) {
                        continue; // Ya fue removido
                    }

                    if (strategy.areDuplicates(a, b)) {
                        // Verificar threshold para fuzzy match
                        if (strategy instanceof FuzzyMatchStrategy fuzzy) {
                            double similarity = fuzzy.computeSimilarity(a.claim(), b.claim());
                            if (similarity < fuzzy.threshold) {
                                continue;
                            }
                        }

                        // Seleccionar ganador
                        KnowledgeFact winner;
                        if (strategy instanceof ExactMatchStrategy exact) {
                            winner = exact.selectWinner(a, b);
                        } else {
                            // Para fuzzy/semantic: el de mayor trust, luego más reciente
                            winner = selectWinner(a, b);
                        }
                        KnowledgeFact loser = (winner == a) ? b : a;

                        // Registrar duplicado
                        groups.computeIfAbsent(winner, k -> new ArrayList<>()).add(loser);
                        remaining.remove(loser);
                        duplicatesRemoved++;
                    }
                }
            }
        }

        return new StrategyResult(remaining, Map.copyOf(groups), duplicatesFound, duplicatesRemoved);
    }

    private KnowledgeFact selectWinner(KnowledgeFact a, KnowledgeFact b) {
        // Prioridad: mayor trust > más reciente > claim más largo
        int trustCmp = Integer.compare(a.trust().ordinal(), b.trust().ordinal());
        if (trustCmp != 0) {
            return trustCmp > 0 ? a : b;
        }
        if (a.publishedAt() != null && b.publishedAt() != null) {
            return a.publishedAt().isAfter(b.publishedAt()) ? a : b;
        }
        return a.claim().length() >= b.claim().length() ? a : b;
    }

    private boolean isEnabled() {
        // TODO: Leer de configuración kin.deduplication.enabled
        return true;
    }
}
