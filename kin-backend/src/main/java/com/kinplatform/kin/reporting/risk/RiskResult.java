package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.engine.EngineResult;

import java.util.List;

/**
 * Resultado inmutable del RiskEngine: lista de riesgos, nivel global,
 * riesgos más críticos y trazabilidad del engine que los produjo.
 *
 * <p>Implementa {@link EngineResult} para compartir el contrato de resultados
 * de la infraestructura común de motores sin perder tipado fuerte.</p>
 */
public record RiskResult(
    List<Risk> risks,
    RiskLevel overallRiskLevel,
    List<Risk> topRisks,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public RiskResult {
        risks = risks == null ? List.of() : List.copyOf(risks);
        topRisks = topRisks == null ? List.of() : List.copyOf(topRisks);
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    public boolean hasRisks() {
        return !risks.isEmpty();
    }

    public int riskCount() {
        return risks.size();
    }

    public RiskLevel highestRiskLevel() {
        return risks.stream()
            .map(Risk::severity)
            .max(Enum::compareTo)
            .orElse(RiskLevel.LOW);
    }

    @Override
    public boolean isEmpty() {
        return risks.isEmpty();
    }

    public static RiskResult empty() {
        return new RiskResult(
            List.of(), RiskLevel.LOW, List.of(), 0.0,
            "No se identificaron riesgos.", "", ""
        );
    }
}
