package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.engine.EngineResult;

import java.util.List;

/**
 * Resultado inmutable del OpportunityEngine: lista de oportunidades,
 * oportunidades prioritarias y trazabilidad del engine que las produjo.
 *
 * <p>Implementa {@link EngineResult} para compartir el contrato de resultados
 * de la infraestructura común de motores sin perder tipado fuerte.</p>
 */
public record OpportunityResult(
    List<Opportunity> opportunities,
    List<Opportunity> topOpportunities,
    double confidence,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public OpportunityResult {
        opportunities = opportunities == null ? List.of() : List.copyOf(opportunities);
        topOpportunities = topOpportunities == null ? List.of() : List.copyOf(topOpportunities);
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    public boolean hasOpportunities() {
        return !opportunities.isEmpty();
    }

    public int opportunityCount() {
        return opportunities.size();
    }

    public int highestPriority() {
        return opportunities.stream()
            .mapToInt(Opportunity::priority)
            .max()
            .orElse(0);
    }

    @Override
    public boolean isEmpty() {
        return opportunities.isEmpty();
    }

    public static OpportunityResult empty() {
        return new OpportunityResult(
            List.of(), List.of(), 0.0,
            "No se identificaron oportunidades.", "", ""
        );
    }
}
