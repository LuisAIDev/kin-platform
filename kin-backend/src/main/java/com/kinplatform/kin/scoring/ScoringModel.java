package com.kinplatform.kin.scoring;

import com.kinplatform.kin.context.AnalyzedDimension;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScoringModel {

    private final Map<AnalyzedDimension, Integer> weights;
    private final String version;
    private final String description;

    public ScoringModel(Map<AnalyzedDimension, Integer> weights, String version, String description) {
        this.weights = Collections.unmodifiableMap(new LinkedHashMap<>(weights));
        this.version = version;
        this.description = description;
    }

    public Map<AnalyzedDimension, Integer> weights() { return weights; }
    public String version() { return version; }
    public String description() { return description; }

    public static ScoringModel defaultModel() {
        var w = new LinkedHashMap<AnalyzedDimension, Integer>();
        w.put(AnalyzedDimension.PROBLEM, 15);
        w.put(AnalyzedDimension.SOLUTION, 15);
        w.put(AnalyzedDimension.TARGET_CUSTOMER, 10);
        w.put(AnalyzedDimension.VALUE_PROPOSITION, 10);
        w.put(AnalyzedDimension.REVENUE_MODEL, 15);
        w.put(AnalyzedDimension.COMPETITION, 10);
        w.put(AnalyzedDimension.RISKS, 8);
        w.put(AnalyzedDimension.RESOURCES, 5);
        w.put(AnalyzedDimension.MVP, 7);
        w.put(AnalyzedDimension.SCALABILITY, 5);
        return new ScoringModel(w, "v1", "Modelo de scoring por dimensiones con pesos configurables");
    }
}
