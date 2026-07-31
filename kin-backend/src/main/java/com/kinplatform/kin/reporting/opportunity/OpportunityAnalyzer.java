package com.kinplatform.kin.reporting.opportunity;

import java.util.List;

/**
 * Contrato común de los analizadores de oportunidades.
 *
 * <p>Cada analizador es un servicio de dominio puro (sin Spring, sin IA, sin
 * infraestructura) especializado en un único tipo de oportunidad. Produce
 * oportunidades 100% explicables y deterministas a partir de {@link OpportunityInput}.</p>
 */
public interface OpportunityAnalyzer {

    /**
     * Categoría de oportunidad que este analizador evalúa.
     */
    OpportunityCategory category();

    /**
     * Evalúa el input y produce las oportunidades de su categoría (0..n).
     */
    List<Opportunity> analyze(OpportunityInput input);

    /**
     * Versión del analizador.
     */
    String version();
}
