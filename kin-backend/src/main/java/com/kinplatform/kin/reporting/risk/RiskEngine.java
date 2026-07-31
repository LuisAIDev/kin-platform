package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Coordinador de analizadores de riesgo.
 *
 * <p>El RiskEngine NO contiene reglas de negocio específicas: únicamente
 * invoca los {@link RiskAnalyzer} registrados (auto-descubiertos a través de
 * la inyección de {@code List<RiskAnalyzer>}, sin listas hardcodeadas),
 * consolida los resultados y calcula métricas agregadas.</p>
 *
 * <p>Servicio de dominio puro: stateless, determinista, sin IA e sin infraestructura.
 * Implementa {@link DomainEngine} para integrarse con la infraestructura común
 * de motores (registry + executor) sin modificar su lógica.</p>
 */
public class RiskEngine implements DomainEngine<RiskInput, RiskResult> {

    public static final String GENERATOR_NAME = "RiskEngine";

    private final List<RiskAnalyzer> analyzers;
    private final RiskModel model;

    public RiskEngine(List<RiskAnalyzer> analyzers, RiskModel model) {
        this.analyzers = new ArrayList<>(analyzers);
        this.model = model;
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(GENERATOR_NAME, model.version(), "KIN Architecture Team",
            EnginePhase.RISK, EngineType.DOMAIN, 50);
    }

    @Override
    public RiskResult evaluate(RiskInput input) {
        if (input == null || input.projectContext() == null
            || input.evaluation() == null || input.score() == null) {
            return RiskResult.empty();
        }

        var risks = new ArrayList<Risk>();
        for (var analyzer : analyzers) {
            risks.addAll(analyzer.analyze(input));
        }

        risks.sort(Comparator.comparingInt(Risk::severityScore).reversed()
            .thenComparing(Comparator.comparingInt(r -> r.category().ordinal()))
            .thenComparing(Risk::title));

        if (risks.isEmpty()) {
            return new RiskResult(List.of(), RiskLevel.LOW, List.of(), 0.0,
                "No se identificaron riesgos.", GENERATOR_NAME, model.version());
        }

        var overall = risks.stream()
            .map(Risk::severity)
            .max(Enum::compareTo)
            .orElse(RiskLevel.LOW);

        var topRisks = risks.size() > 3 ? List.copyOf(risks.subList(0, 3)) : List.copyOf(risks);

        double confidence = risks.stream()
            .mapToDouble(Risk::confidence)
            .average()
            .orElse(0.0);

        var explanation = buildExplanation(risks);

        return new RiskResult(risks, overall, topRisks, confidence,
            explanation, GENERATOR_NAME, model.version());
    }

    public List<RiskAnalyzer> analyzers() {
        return List.copyOf(analyzers);
    }

    private String buildExplanation(List<Risk> risks) {
        var business = countByCategory(risks, RiskCategory.BUSINESS);
        var technical = countByCategory(risks, RiskCategory.TECHNICAL);
        var financial = countByCategory(risks, RiskCategory.FINANCIAL);
        var market = countByCategory(risks, RiskCategory.MARKET);
        return "Se identificaron " + risks.size() + " riesgos: " + business + " de negocio, "
            + technical + " técnicos, " + financial + " financieros, " + market + " de mercado.";
    }

    private long countByCategory(List<Risk> risks, RiskCategory category) {
        return risks.stream().filter(r -> r.category() == category).count();
    }
}
