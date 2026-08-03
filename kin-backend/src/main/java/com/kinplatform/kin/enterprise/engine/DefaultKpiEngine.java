package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.KpiInput;
import com.kinplatform.kin.enterprise.engine.result.KpiResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.KpiSet;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;

import java.util.List;

/**
 * Implementación determinista del {@link KpiEngine} (Fase 10, Milestone 2D).
 *
 * <p>Define los KPIs por fase del ciclo de vida del cliente y financiera
 * (adquisición, activación, retención, ingresos y financieros) a partir del
 * plan de mercado y del plan financiero. Reglas funcionales aplicadas:</p>
 *
 * <ul>
 *   <li><b>Adquisición</b>: objetivo = mercado obtenible (SOM) del plan de
 *       mercado, fórmula {@code "SOM anual"} y frecuencia anual.</li>
 *   <li><b>Activación</b>: objetivo = {@link #ACTIVATION_TARGET} (tasa de
 *       activación objetivo por defecto, documentada).</li>
 *   <li><b>Retención</b>: objetivo = {@link #RETENTION_TARGET} (tasa de
 *       retención objetivo por defecto, documentada).</li>
 *   <li><b>Ingresos</b>: objetivo = ingreso del año 3 del plan financiero.</li>
 *   <li><b>Financieros</b>: objetivo = margen bruto del plan financiero.</li>
 *   <li>El valor actual queda en {@code 0} (sin mediciones reales) y la
 *       confianza se hereda del plan de mercado.</li>
 * </ul>
 *
 * <p>Motor stateless, thread-safe, sin dependencias, sin Spring y sin efectos
 * secundarios.</p>
 */
public class DefaultKpiEngine implements KpiEngine {

    private static final String ENGINE_NAME = "kin.enterprise:Kpi";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String ENGINE_AUTHOR = "KIN Architecture Team";
    private static final int ENGINE_PRIORITY = 86;

    /** Tasa de activación objetivo por defecto (30%). */
    static final double ACTIVATION_TARGET = 0.30;
    /** Tasa de retención objetivo por defecto (80%). */
    static final double RETENTION_TARGET = 0.80;
    private static final double CURRENT_UNKNOWN = 0.0;
    private static final String FREQUENCY_ANNUAL = "Anual";
    private static final String FREQUENCY_MONTHLY = "Mensual";

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(ENGINE_NAME, ENGINE_VERSION, ENGINE_AUTHOR,
            EnginePhase.FINANCIAL, EngineType.DOMAIN, ENGINE_PRIORITY);
    }

    @Override
    public KpiResult evaluate(KpiInput input) {
        if (input == null || input.marketPlan() == null || input.financialPlan() == null) {
            return KpiResult.empty();
        }
        MarketPlan market = input.marketPlan();
        FinancialPlan plan = input.financialPlan();

        List<KpiSet.Kpi> acquisition = List.of(KpiSet.Kpi.of(
            "Adquisición de clientes", market.som(), CURRENT_UNKNOWN,
            "SOM anual", FREQUENCY_ANNUAL));
        List<KpiSet.Kpi> activation = List.of(KpiSet.Kpi.of(
            "Activación", ACTIVATION_TARGET, CURRENT_UNKNOWN,
            "Activos / registrados", FREQUENCY_MONTHLY));
        List<KpiSet.Kpi> retention = List.of(KpiSet.Kpi.of(
            "Retención", RETENTION_TARGET, CURRENT_UNKNOWN,
            "Retenidos / activos", FREQUENCY_MONTHLY));
        List<KpiSet.Kpi> revenue = List.of(KpiSet.Kpi.of(
            "Ingresos año 3", plan.revenueYear3(), CURRENT_UNKNOWN,
            "Proyección financiera", FREQUENCY_ANNUAL));
        List<KpiSet.Kpi> financial = List.of(KpiSet.Kpi.of(
            "Margen bruto", plan.grossMargin(), CURRENT_UNKNOWN,
            "Ingresos - costes variables / ingresos", FREQUENCY_MONTHLY));

        var kpis = KpiSet.of(acquisition, activation, retention, revenue, financial);

        double confidence = market.confidence();
        String explanation = "KPIs definidos a partir del plan de mercado y del plan financiero.";

        return new KpiResult(kpis, confidence, explanation,
            "KpiEngine", ENGINE_VERSION);
    }
}
