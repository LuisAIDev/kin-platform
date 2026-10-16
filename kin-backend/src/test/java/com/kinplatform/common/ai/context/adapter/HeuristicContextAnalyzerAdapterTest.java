package com.kinplatform.common.ai.context.adapter;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.context.AnalysisResult;
import com.kinplatform.common.context.AnalyzedDimension;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Reproduce y bloquea la regresión de la Fase 7.5b.1 sobre el mensaje E2E que
 * falló en producción: falsos positivos de SECTOR, modelo de ingresos con
 * porcentajes, escalabilidad con verbo conector y montos con decimales.
 */
class HeuristicContextAnalyzerAdapterTest {

    private static final String E2E_MESSAGE =
            "MVP es un diagnóstico pago de 3.5 millones COP. "
                    + "El modelo de ingresos es 60% retainers, 30% proyectos, 10% capacitaciones. "
                    + "La competencia es Deloitte y PwC por arriba, freelancers por abajo. "
                    + "Los riesgos son dependencia de 3 consultores senior. "
                    + "La escalabilidad depende de documentar playbooks. "
                    + "Los objetivos son 5 diagnósticos en 3 meses, 8 en 6 meses.";

    private final HeuristicContextAnalyzerAdapter adapter = new HeuristicContextAnalyzerAdapter();

    private Map<AnalyzedDimension, String> analyze(String message) {
        AnalysisResult result = adapter.analyze(message, null);
        return result.extracted();
    }

    @Test
    void e2e_corrigeLosCuatroBugs() {
        var extracted = analyze(E2E_MESSAGE);

        assertNull(extracted.get(AnalyzedDimension.SECTOR),
                "B1: SECTOR no debe capturar 'es un diagnóstico...'");

        String revenue = extracted.get(AnalyzedDimension.REVENUE_MODEL);
        assertTrue(revenue != null && revenue.contains("60%"),
                "B2: REVENUE_MODEL debe capturar '60%'");
        assertTrue(revenue != null && revenue.contains("retainers"),
                "B2: REVENUE_MODEL debe capturar 'retainers'");

        String scalability = extracted.get(AnalyzedDimension.SCALABILITY);
        assertTrue(scalability != null && scalability.contains("playbooks"),
                "B3: SCALABILITY debe capturar 'playbooks'");

        String mvp = extracted.get(AnalyzedDimension.MVP);
        assertTrue(mvp != null && mvp.contains("3.5"),
                "B4: MVP no debe truncarse en el decimal '3.5'");
    }

    @Test
    void valueProposition_seExtraeCuandoEstaPresente() {
        var extracted = analyze(
                "Nuestra propuesta de valor es metodología de consultora grande a precio de PYME.");

        String valueProposition = extracted.get(AnalyzedDimension.VALUE_PROPOSITION);
        assertTrue(valueProposition != null && valueProposition.contains("precio de PYME"),
                "VALUE_PROPOSITION debe capturar el texto completo");
    }
}
