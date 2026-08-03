package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.FinancialPlanInput;
import com.kinplatform.kin.enterprise.engine.result.FinancialPlanResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;

/**
 * Implementación determinista del {@link FinancialPlanEngine} (Fase 10,
 * Milestone 2D).
 *
 * <p>Genera el {@link FinancialPlan} con sus tres escenarios (base, optimista y
 * pesimista) a partir del plan de mercado. Todos los cálculos son
 * deterministas: no se usan números aleatorios ni llamadas externas. Las
 * constantes del modelo están documentadas a continuación.</p>
 *
 * <h2>Fórmulas aprobadas (constantes del modelo)</h2>
 *
 * <ul>
 *   <li>{@code revenueYear1 = SOM} (el mercado obtenible es la base de
 *       ingresos del primer año).</li>
 *   <li>{@code revenueYear2 = revenueYear1 * (1 + growthRate/100)} y
 *       {@code revenueYear3 = revenueYear2 * (1 + growthRate/100)} donde
 *       {@code growthRate} proviene del plan de mercado.</li>
 *   <li>{@code capex = revenueYear1 * CAPEX_RATIO} con
 *       {@code CAPEX_RATIO = 0.25} (25% de los ingresos del año 1).</li>
 *   <li>{@code opex = revenueYear1 * OPEX_RATIO} con
 *       {@code OPEX_RATIO = 0.40} (40% de los ingresos del año 1).</li>
 *   <li>{@code grossMargin = 60.0} (margen bruto objetivo por defecto,
 *       documentado).</li>
 *   <li>{@code breakEvenMonth} = mes en que la contribución anual neta
 *       acumulada cubre el CAPEX:
 *       {@code ceil(capex * 12 / (revenueYear1 - opex))}; si el
 *       denominador es &le; 0, no se alcanza el punto de equilibrio
 *       ({@code 0}).</li>
 *   <li>Escenarios: {@code optimistic} = ingresos de año 3 ×
 *       {@code OPTIMISTIC_UPLIFT = 1.25} y margen + {@code MARGIN_DELTA = 10};
 *       {@code pessimistic} = ingresos de año 3 ×
 *       {@code PESSIMISTIC_DOWNSHIFT = 0.75} y margen −
 *       {@code MARGIN_DELTA = 10}.</li>
 *   <li>La confianza se hereda del plan de mercado.</li>
 * </ul>
 *
 * <p>Motor stateless, thread-safe, sin dependencias, sin Spring y sin efectos
 * secundarios.</p>
 */
public class DefaultFinancialPlanEngine implements FinancialPlanEngine {

    private static final String ENGINE_NAME = "kin.enterprise:FinancialPlan";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String ENGINE_AUTHOR = "KIN Architecture Team";
    private static final int ENGINE_PRIORITY = 83;

    /** Proporción de los ingresos del año 1 destinada a inversión en capital. */
    static final double CAPEX_RATIO = 0.25;
    /** Proporción de los ingresos del año 1 destinada a gasto operativo anual. */
    static final double OPEX_RATIO = 0.40;
    /** Margen bruto objetivo por defecto (porcentaje). */
    static final double DEFAULT_GROSS_MARGIN = 60.0;
    /** Multiplicador de ingresos del escenario optimista. */
    static final double OPTIMISTIC_UPLIFT = 1.25;
    /** Multiplicador de ingresos del escenario pesimista. */
    static final double PESSIMISTIC_DOWNSHIFT = 0.75;
    /** Variación de margen (puntos porcentuales) entre escenarios. */
    static final double MARGIN_DELTA = 10.0;
    private static final int MONTHS_PER_YEAR = 12;

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(ENGINE_NAME, ENGINE_VERSION, ENGINE_AUTHOR,
            EnginePhase.FINANCIAL, EngineType.DOMAIN, ENGINE_PRIORITY);
    }

    @Override
    public FinancialPlanResult evaluate(FinancialPlanInput input) {
        if (input == null || input.marketPlan() == null || input.context() == null) {
            return FinancialPlanResult.empty();
        }
        MarketPlan market = input.marketPlan();

        double revenueYear1 = market.som();
        double growthRate = market.growthRate();
        double revenueYear2 = revenueYear1 * (1.0 + growthRate / 100.0);
        double revenueYear3 = revenueYear2 * (1.0 + growthRate / 100.0);

        double capex = revenueYear1 * CAPEX_RATIO;
        double opex = revenueYear1 * OPEX_RATIO;
        double grossMargin = DEFAULT_GROSS_MARGIN;
        int breakEvenMonth = breakEvenMonth(capex, opex, revenueYear1);

        FinancialPlan.Scenario base = FinancialPlan.Scenario.of(revenueYear3, grossMargin);
        FinancialPlan.Scenario optimistic = FinancialPlan.Scenario.of(
            round2(revenueYear3 * OPTIMISTIC_UPLIFT), Math.min(100.0, grossMargin + MARGIN_DELTA));
        FinancialPlan.Scenario pessimistic = FinancialPlan.Scenario.of(
            round2(revenueYear3 * PESSIMISTIC_DOWNSHIFT), Math.max(0.0, grossMargin - MARGIN_DELTA));

        var plan = FinancialPlan.of(round2(capex), round2(opex),
            round2(revenueYear1), round2(revenueYear2), round2(revenueYear3),
            breakEvenMonth, grossMargin, optimistic, base, pessimistic);

        double confidence = market.confidence();
        String explanation = buildExplanation(revenueYear1, breakEvenMonth);

        return new FinancialPlanResult(plan, confidence, explanation,
            "FinancialPlanEngine", ENGINE_VERSION);
    }

    private int breakEvenMonth(double capex, double opex, double revenueYear1) {
        double annualContribution = revenueYear1 - opex;
        if (annualContribution <= 0.0) {
            return 0;
        }
        return (int) Math.ceil(capex * MONTHS_PER_YEAR / annualContribution);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String buildExplanation(double revenueYear1, int breakEvenMonth) {
        if (breakEvenMonth == 0) {
            return "El plan no alcanza el punto de equilibrio dentro del horizonte analizado.";
        }
        return "Plan financiero a tres años basado en un mercado obtenible de "
            + revenueYear1 + " con punto de equilibrio en el mes " + breakEvenMonth + ".";
    }
}
