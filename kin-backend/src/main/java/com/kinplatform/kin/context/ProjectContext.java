package com.kinplatform.kin.context;

import com.kinplatform.kin.decision.ConversationDecision;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectContext {

    private final Map<AnalyzedDimension, String> data = new EnumMap<>(AnalyzedDimension.class);
    private final Set<AnalyzedDimension> dimensionsCovered = EnumSet.noneOf(AnalyzedDimension.class);
    private ConversationDecision currentDecision;
    private int exchangeCount;
    private boolean reportGenerated;

    public static ProjectContext fromProject(String title, String description, String category) {
        var ctx = new ProjectContext();
        if (title != null && !title.isBlank()) {
            ctx.data.put(AnalyzedDimension.PROJECT_NAME, title.trim());
            ctx.dimensionsCovered.add(AnalyzedDimension.PROJECT_NAME);
        }
        if (category != null && !category.isBlank()) {
            ctx.data.put(AnalyzedDimension.SECTOR, category.trim());
            ctx.dimensionsCovered.add(AnalyzedDimension.SECTOR);
        }
        if (description != null && !description.isBlank()) {
            ctx.data.put(AnalyzedDimension.SOLUTION, description.trim());
        }
        return ctx;
    }

    public void update(AnalysisResult result) {
        for (var entry : result.extracted().entrySet()) {
            var value = entry.getValue();
            if (value != null && !value.isBlank()) {
                data.put(entry.getKey(), value.strip());
                dimensionsCovered.add(entry.getKey());
            }
        }
        exchangeCount++;
    }

    public boolean isDimensionCovered(AnalyzedDimension dimension) {
        return dimensionsCovered.contains(dimension);
    }

    public double coverageRatio() {
        return (double) dimensionsCovered.size() / AnalyzedDimension.values().length;
    }

    public boolean hasKnownDimensions() {
        return !data.isEmpty();
    }

    public int exchangeCount() {
        return exchangeCount;
    }

    public boolean reportGenerated() {
        return reportGenerated;
    }

    public void markReportGenerated() {
        this.reportGenerated = true;
    }

    public void attachDecision(ConversationDecision decision) {
        this.currentDecision = decision;
    }

    public ConversationDecision currentDecision() {
        return currentDecision;
    }

    public String value(AnalyzedDimension dimension) {
        return data.get(dimension);
    }

    public Set<AnalyzedDimension> coveredDimensions() {
        return EnumSet.copyOf(dimensionsCovered);
    }

    public List<AnalyzedDimension> missingDimensions() {
        var missing = new ArrayList<AnalyzedDimension>();
        for (var dim : AnalyzedDimension.values()) {
            if (!dimensionsCovered.contains(dim)) {
                missing.add(dim);
            }
        }
        return missing;
    }

    public String toPromptSnippet() {
        if (data.isEmpty()) return "";
        var sb = new StringBuilder();
        for (var entry : data.entrySet()) {
            sb.append("- ").append(entry.getKey().displayName())
              .append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    public int knownDimensionsCount() {
        return dimensionsCovered.size();
    }
}
