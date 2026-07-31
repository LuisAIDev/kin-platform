package com.kinplatform.kin.reporting.risk;

import java.util.List;

/**
 * Contrato común de los analizadores de riesgo.
 *
 * <p>Cada analizador es un servicio de dominio puro (sin Spring, sin IA, sin
 * infraestructura) especializado en un único tipo de riesgo. Produce riesgos
 * 100% explicables y deterministas a partir de {@link RiskInput}.</p>
 */
public interface RiskAnalyzer {

    /**
     * Categoría de riesgo que este analizador evalúa.
     */
    RiskCategory category();

    /**
     * Evalúa el input y produce los riesgos de su categoría (0..n).
     */
    List<Risk> analyze(RiskInput input);

    /**
     * Versión del analizador.
     */
    String version();
}
