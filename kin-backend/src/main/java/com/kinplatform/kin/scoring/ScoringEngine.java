package com.kinplatform.kin.scoring;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor determinista de scoring de viabilidad. Evalúa el contexto del proyecto
 * y la evaluación de completitud para producir un puntaje, desglose por
 * dimensión, fortalezas y debilidades.
 *
 * <p>Servicio de dominio puro (sin Spring, sin IA): implementa
 * {@link DomainEngine} para integrarse con la infraestructura común de motores
 * (registry + executor) sin modificar su lógica.</p>
 */
public class ScoringEngine implements DomainEngine<ScoringInput, ScoreResult> {

    public static final String GENERATOR_NAME = "ScoringEngine";
    public static final String ENGINE_VERSION = "v1";

    private final ScoringModel model;

    public ScoringEngine(ScoringModel model) {
        this.model = model;
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(GENERATOR_NAME, model.version(), "KIN Architecture Team",
            EnginePhase.SCORING, EngineType.DOMAIN, 30);
    }

    @Override
    public ScoreResult evaluate(ScoringInput input) {
        if (input == null || input.projectContext() == null || input.evaluation() == null) {
            return ScoreResult.empty();
        }
        return evaluate(input.projectContext(), input.evaluation());
    }

    public ScoreResult evaluate(ProjectContext context, CompletenessEvaluation evaluation) {
        var weights = model.weights();
        var categoryScores = new LinkedHashMap<String, Integer>();

        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        int earned = 0;

        for (var entry : weights.entrySet()) {
            var dimension = entry.getKey();
            var weight = entry.getValue();
            int score = scoreDimension(context, dimension, evaluation);
            categoryScores.put(dimension.displayName(), score);
            earned += score;
        }

        int totalScore = Math.min(100, (int) Math.round((double) earned / totalWeight * 100));
        String viability = determineViability(totalScore, evaluation);

        return new ScoreResult(
            totalScore,
            100,
            categoryScores,
            viability,
            identifyStrengths(context, evaluation),
            identifyWeaknesses(context, evaluation),
            ""
        );
    }

    private int scoreDimension(ProjectContext context, AnalyzedDimension dimension,
                                CompletenessEvaluation evaluation) {
        if (!context.isDimensionCovered(dimension)) return 0;
        String value = context.value(dimension);
        if (value == null || value.isBlank()) return 0;
        int length = value.strip().length();
        if (length < 20) return 3;
        if (length < 50) return 5;
        if (length < 100) return 7;
        return 10;
    }

    private String determineViability(int score, CompletenessEvaluation evaluation) {
        double coverage = evaluation.coveragePercent();
        if (score >= 80 && coverage >= 0.7) return "MUY_ALTA";
        if (score >= 60 && coverage >= 0.5) return "ALTA";
        if (score >= 40 && coverage >= 0.3) return "MEDIA";
        return "BAJA";
    }

    private List<String> identifyStrengths(ProjectContext context, CompletenessEvaluation evaluation) {
        var strengths = new ArrayList<String>();
        if (context.isDimensionCovered(AnalyzedDimension.PROBLEM)) {
            strengths.add("Problema claramente definido");
        }
        if (context.isDimensionCovered(AnalyzedDimension.SOLUTION)) {
            strengths.add("Soluci\u00F3n propuesta documentada");
        }
        if (context.isDimensionCovered(AnalyzedDimension.TARGET_CUSTOMER)) {
            strengths.add("Cliente objetivo identificado");
        }
        if (context.isDimensionCovered(AnalyzedDimension.REVENUE_MODEL)) {
            strengths.add("Modelo de ingresos definido");
        }
        if (evaluation.coveragePercent() >= 0.6) {
            strengths.add("Buena cobertura de dimensiones del proyecto");
        }
        return strengths;
    }

    private List<String> identifyWeaknesses(ProjectContext context, CompletenessEvaluation evaluation) {
        var weaknesses = new ArrayList<String>();
        if (!context.isDimensionCovered(AnalyzedDimension.COMPETITION)) {
            weaknesses.add("Falta an\u00E1lisis de competencia");
        }
        if (!context.isDimensionCovered(AnalyzedDimension.RISKS)) {
            weaknesses.add("Riesgos no evaluados");
        }
        if (!context.isDimensionCovered(AnalyzedDimension.MVP)) {
            weaknesses.add("Plan de validaci\u00F3n no definido");
        }
        if (!context.isDimensionCovered(AnalyzedDimension.SCALABILITY)) {
            weaknesses.add("Escalabilidad no evaluada");
        }
        if (evaluation.coveragePercent() < 0.4) {
            weaknesses.add("Informaci\u00F3n insuficiente para evaluaci\u00F3n completa");
        }
        return weaknesses;
    }
}
