package com.kinplatform.kin.context;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ExplorationPriority {

    private final Map<AnalyzedDimension, Integer> priorities;

    public ExplorationPriority(Map<AnalyzedDimension, Integer> priorities) {
        this.priorities = new EnumMap<>(priorities);
    }

    public int getPriority(AnalyzedDimension dimension) {
        return priorities.getOrDefault(dimension, 5);
    }

    public AnalyzedDimension highestPriority(List<AnalyzedDimension> candidates) {
        return candidates.stream()
            .max(Comparator.comparingInt(d -> priorities.getOrDefault(d, 0)))
            .orElse(null);
    }

    public static ExplorationPriority defaultPriorities() {
        var p = new EnumMap<AnalyzedDimension, Integer>(AnalyzedDimension.class);
        p.put(AnalyzedDimension.PROBLEM, 10);
        p.put(AnalyzedDimension.SOLUTION, 9);
        p.put(AnalyzedDimension.TARGET_CUSTOMER, 8);
        p.put(AnalyzedDimension.REVENUE_MODEL, 8);
        p.put(AnalyzedDimension.VALUE_PROPOSITION, 7);
        p.put(AnalyzedDimension.COMPETITION, 7);
        p.put(AnalyzedDimension.MVP, 7);
        p.put(AnalyzedDimension.RISKS, 6);
        p.put(AnalyzedDimension.RESOURCES, 5);
        p.put(AnalyzedDimension.SCALABILITY, 5);
        p.put(AnalyzedDimension.OBJECTIVES, 4);
        p.put(AnalyzedDimension.SECTOR, 3);
        p.put(AnalyzedDimension.CITY, 2);
        p.put(AnalyzedDimension.PROJECT_NAME, 1);
        return new ExplorationPriority(p);
    }
}
