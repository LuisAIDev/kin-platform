package com.kinplatform.kin.reporting.report.model;

import java.util.List;
import java.util.Map;

/**
 * Sección de puntuación del reporte: proyección directa del {@code ScoreResult}.
 */
public record ScoresSection(
    int totalScore,
    int maxScore,
    Map<String, Integer> categoryScores,
    String viabilityLabel,
    double confidenceLevel,
    List<String> strengths,
    List<String> weaknesses,
    String scoringModelVersion
) implements ReportSection {

    public ScoresSection {
        totalScore = Math.max(0, totalScore);
        maxScore = Math.max(0, maxScore);
        categoryScores = categoryScores == null ? Map.of() : Map.copyOf(categoryScores);
        viabilityLabel = viabilityLabel == null ? "" : viabilityLabel;
        confidenceLevel = Math.max(0.0, Math.min(100.0, confidenceLevel));
        strengths = strengths == null ? List.of() : List.copyOf(strengths);
        weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
        scoringModelVersion = scoringModelVersion == null ? "" : scoringModelVersion;
    }

    @Override
    public String sectionName() {
        return "Scores";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.SCORING;
    }

    public boolean isEmpty() {
        return totalScore == 0 && categoryScores.isEmpty();
    }

    public static ScoresSection empty() {
        return new ScoresSection(0, 0, Map.of(), "", 0.0, List.of(), List.of(), "");
    }
}
