package com.kinplatform.kin.scoring;

import com.kinplatform.kin.engine.EngineResult;

import java.util.List;
import java.util.Map;

/**
 * Resultado inmutable del ScoringEngine: puntaje total, desglose por categoría,
 * etiqueta de viabilidad, fortalezas/debilidades y explicación.
 *
 * <p>Implementa {@link EngineResult} para compartir el contrato de resultados
 * de la infraestructura común de motores sin perder tipado fuerte.</p>
 */
public record ScoreResult(
    int totalScore,
    int maxScore,
    Map<String, Integer> categoryScores,
    String viabilityLabel,
    List<String> strengths,
    List<String> weaknesses,
    String explanation
) implements EngineResult {

    public static ScoreResult empty() {
        return new ScoreResult(0, 100, Map.of(), "NO_EVALUATED", List.of(), List.of(), "");
    }

    @Override
    public double confidence() {
        return maxScore > 0 ? (double) totalScore / maxScore : 0.0;
    }

    @Override
    public String generatedBy() {
        return ScoringEngine.GENERATOR_NAME;
    }

    @Override
    public String engineVersion() {
        return ScoringEngine.ENGINE_VERSION;
    }

    @Override
    public boolean isEmpty() {
        return totalScore == 0 && categoryScores.isEmpty();
    }
}
