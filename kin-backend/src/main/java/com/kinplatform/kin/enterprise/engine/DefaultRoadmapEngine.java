package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.RoadmapInput;
import com.kinplatform.kin.enterprise.engine.result.RoadmapResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.Roadmap;
import com.kinplatform.kin.reporting.RecommendationResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación determinista del {@link RoadmapEngine} (Fase 10,
 * Milestone 2D).
 *
 * <p>Construye el {@link Roadmap} con fases, hitos, dependencias y entradas de
 * Gantt a partir de las recomendaciones del pipeline. Las fechas son relativas
 * (meses 1-indexados). El horizonte se alinea al plan financiero cuando existe
 * ({@code breakEvenMonth}). Reglas funcionales aplicadas:</p>
 *
 * <ul>
 *   <li>Fases derivadas de las categorías de las recomendaciones, en orden
 *       canónico de ejecución ({@code VALIDATION} → {@code PRODUCT} →
 *       {@code MARKETING} → {@code OPERATIONS} → {@code STRATEGY} →
 *       {@code FINANCIAL} → {@code TEAM} → {@code INNOVATION}).</li>
 *   <li>Cada fase ocupa un bloque de {@code MONTHS_PER_PHASE} meses
 *       consecutivos y produce una entrada de Gantt.</li>
 *   <li>Hitos: un hito por fase con el mes de finalización relativo.</li>
 *   <li>Dependencias: cada fase (salvo la primera) depende de la anterior.</li>
 *   <li>Si no hay recomendaciones, el roadmap se construye en modo vacío con
 *       una fase de validación por defecto, cronograma {@code "Por definir"} y
 *       confianza cero.</li>
 * </ul>
 *
 * <p>Motor stateless, thread-safe, sin dependencias, sin Spring y sin efectos
 * secundarios.</p>
 */
public class DefaultRoadmapEngine implements RoadmapEngine {

    static final String UNDEFINED = "Por definir";

    private static final String ENGINE_NAME = "kin.enterprise:Roadmap";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String ENGINE_AUTHOR = "KIN Architecture Team";
    private static final int ENGINE_PRIORITY = 84;

    private static final int MONTHS_PER_PHASE = 3;
    private static final int DEFAULT_HORIZON_MONTHS = 12;

    private static final List<String> PHASE_ORDER =
            List.of("VALIDATION", "PRODUCT", "MARKETING", "OPERATIONS", "STRATEGY", "FINANCIAL", "TEAM", "INNOVATION");

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(
                ENGINE_NAME,
                ENGINE_VERSION,
                ENGINE_AUTHOR,
                EnginePhase.EXPLANATION,
                EngineType.DOMAIN,
                ENGINE_PRIORITY);
    }

    @Override
    public RoadmapResult evaluate(RoadmapInput input) {
        if (input == null || input.context() == null) {
            return RoadmapResult.empty();
        }
        var recommendations = input.recommendations();
        var financialPlan = input.financialPlan();

        int horizon = horizon(financialPlan);
        boolean hasRecommendations = recommendations != null && recommendations.hasRecommendations();
        List<String> phases = phases(recommendations);
        List<String> milestones = milestones(phases, horizon);
        List<String> dependencies = dependencies(phases);
        List<Roadmap.GanttEntry> gantt = ganttEntries(phases, horizon);

        String timeline = hasRecommendations ? buildTimeline(phases, horizon) : UNDEFINED;

        var roadmap = Roadmap.of(phases, milestones, timeline, dependencies, gantt);

        double confidence = recommendations == null ? 0.0 : recommendations.confidence();
        String explanation = buildExplanation(phases, horizon);

        return new RoadmapResult(roadmap, confidence, explanation, "RoadmapEngine", ENGINE_VERSION);
    }

    private int horizon(FinancialPlan financialPlan) {
        if (financialPlan == null || financialPlan.breakEvenMonth() <= 0) {
            return DEFAULT_HORIZON_MONTHS;
        }
        return Math.max(DEFAULT_HORIZON_MONTHS, financialPlan.breakEvenMonth());
    }

    private List<String> phases(RecommendationResult recommendations) {
        Set<String> present = new LinkedHashSet<>();
        if (recommendations != null) {
            for (var recommendation : recommendations.recommendations()) {
                var category = recommendation.category();
                if (category != null && PHASE_ORDER.contains(category.name())) {
                    present.add(category.name());
                }
            }
        }
        if (present.isEmpty()) {
            return List.of("Validación");
        }
        var phases = new ArrayList<String>();
        for (String phase : PHASE_ORDER) {
            if (present.contains(phase)) {
                phases.add(phase.toLowerCase());
            }
        }
        return List.copyOf(phases);
    }

    private List<String> milestones(List<String> phases, int horizon) {
        if (phases.isEmpty()) {
            return List.of();
        }
        var milestones = new ArrayList<String>();
        int start = 1;
        for (String phase : phases) {
            int end = Math.min(start + MONTHS_PER_PHASE - 1, horizon);
            milestones.add(phase + " completado (mes " + end + ")");
            start = end + 1;
        }
        return List.copyOf(milestones);
    }

    private List<String> dependencies(List<String> phases) {
        var dependencies = new ArrayList<String>();
        for (int i = 1; i < phases.size(); i++) {
            dependencies.add(phases.get(i - 1) + " precede a " + phases.get(i));
        }
        return List.copyOf(dependencies);
    }

    private List<Roadmap.GanttEntry> ganttEntries(List<String> phases, int horizon) {
        var entries = new ArrayList<Roadmap.GanttEntry>();
        int start = 1;
        for (String phase : phases) {
            int end = Math.min(start + MONTHS_PER_PHASE - 1, horizon);
            entries.add(Roadmap.GanttEntry.of(phase, start, end));
            start = end + 1;
        }
        return List.copyOf(entries);
    }

    private String buildTimeline(List<String> phases, int horizon) {
        if (phases.isEmpty()) {
            return UNDEFINED;
        }
        return "Horizonte de " + horizon + " meses en " + phases.size() + " fase(s).";
    }

    private String buildExplanation(List<String> phases, int horizon) {
        if (phases.isEmpty()) {
            return "Sin recomendaciones previas: roadmap por defecto.";
        }
        return "Roadmap de " + phases.size() + " fases sobre un horizonte de " + horizon + " meses.";
    }
}
