package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Coordinador de analizadores de oportunidades.
 *
 * <p>El OpportunityEngine NO contiene reglas de negocio específicas:
 * únicamente invoca los {@link OpportunityAnalyzer} registrados
 * (auto-descubiertos a través de la inyección de {@code List<OpportunityAnalyzer>}),
 * consolida los resultados y calcula métricas agregadas.</p>
 *
 * <p>Servicio de dominio puro: stateless, determinista, sin IA e sin
 * infraestructura. Implementa {@link DomainEngine} para integrarse con la
 * infraestructura común de motores (registry + executor) sin modificar su
 * lógica.</p>
 */
public class OpportunityEngine implements DomainEngine<OpportunityInput, OpportunityResult> {

    public static final String GENERATOR_NAME = "OpportunityEngine";

    private final List<OpportunityAnalyzer> analyzers;
    private final OpportunityModel model;

    public OpportunityEngine(List<OpportunityAnalyzer> analyzers, OpportunityModel model) {
        this.analyzers = new ArrayList<>(analyzers);
        this.model = model;
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(GENERATOR_NAME, model.version(), "KIN Architecture Team",
            EnginePhase.OPPORTUNITY, EngineType.DOMAIN, 60);
    }

    @Override
    public OpportunityResult evaluate(OpportunityInput input) {
        if (input == null || input.projectContext() == null
            || input.evaluation() == null || input.score() == null) {
            return OpportunityResult.empty();
        }

        var opportunities = new ArrayList<Opportunity>();
        for (var analyzer : analyzers) {
            opportunities.addAll(analyzer.analyze(input));
        }

        opportunities.sort(Comparator.comparingInt(Opportunity::priority).reversed()
            .thenComparing(Comparator.comparingInt(o -> o.category().ordinal()))
            .thenComparing(Opportunity::title));

        if (opportunities.isEmpty()) {
            return new OpportunityResult(List.of(), List.of(), 0.0,
                "No se identificaron oportunidades.", GENERATOR_NAME, model.version());
        }

        var top = opportunities.size() > 3
            ? List.copyOf(opportunities.subList(0, 3))
            : List.copyOf(opportunities);

        double confidence = opportunities.stream()
            .mapToDouble(Opportunity::confidence)
            .average()
            .orElse(0.0);

        var explanation = buildExplanation(opportunities);

        return new OpportunityResult(opportunities, top, confidence,
            explanation, GENERATOR_NAME, model.version());
    }

    public List<OpportunityAnalyzer> analyzers() {
        return List.copyOf(analyzers);
    }

    private String buildExplanation(List<Opportunity> opportunities) {
        var sb = new StringBuilder("Se identificaron ").append(opportunities.size())
            .append(" oportunidades de mejora:");
        for (var category : OpportunityCategory.values()) {
            long count = opportunities.stream()
                .filter(o -> o.category() == category)
                .count();
            if (count > 0) {
                sb.append(' ').append(count).append(" de ").append(category.displayName()).append(',');
            }
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        sb.append('.');
        return sb.toString();
    }
}
