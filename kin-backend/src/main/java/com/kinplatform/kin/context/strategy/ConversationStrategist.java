package com.kinplatform.kin.context.strategy;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ConversationStrategist {

    private static final Logger log = LoggerFactory.getLogger(ConversationStrategist.class);

    private final ExplorationStrategy defaultStrategy;
    private final Map<String, ExplorationStrategy> specializedStrategies;

    public ConversationStrategist(ExplorationStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        this.specializedStrategies = new HashMap<>();
    }

    public ConversationStrategist(ExplorationStrategy defaultStrategy, Map<String, ExplorationStrategy> specializedStrategies) {
        this.defaultStrategy = defaultStrategy;
        this.specializedStrategies = new HashMap<>(specializedStrategies);
    }

    public void registerStrategy(String sectorKey, ExplorationStrategy strategy) {
        specializedStrategies.put(sectorKey.toLowerCase(), strategy);
    }

    public ConversationDecision decide(ProjectContext context, CompletenessEvaluation evaluation) {
        var selected = selectStrategy(context);
        var decision = selected.decide(context, evaluation);

        log.info("=== STRATEGY DECISION === selected={}, action={}, nextDim={}, priority={}",
            selected.getClass().getSimpleName(),
            decision.action(),
            decision.dimension() != null ? decision.dimension().displayName() : "N/A",
            decision.priority());

        return decision;
    }

    private ExplorationStrategy selectStrategy(ProjectContext context) {
        var sector = context.value(AnalyzedDimension.SECTOR);
        if (sector != null) {
            var lower = sector.toLowerCase();
            if (specializedStrategies.containsKey(lower)) {
                return specializedStrategies.get(lower);
            }
            for (var entry : specializedStrategies.entrySet()) {
                if (lower.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return defaultStrategy;
    }
}
