package com.kinplatform.kin.scoring;

import java.util.List;
import java.util.Map;

public record ScoreResult(
    int totalScore,
    int maxScore,
    Map<String, Integer> categoryScores,
    String viabilityLabel,
    List<String> strengths,
    List<String> weaknesses,
    String explanation
) {

    public static ScoreResult empty() {
        return new ScoreResult(0, 100, Map.of(), "NO_EVALUATED", List.of(), List.of(), "");
    }
}
