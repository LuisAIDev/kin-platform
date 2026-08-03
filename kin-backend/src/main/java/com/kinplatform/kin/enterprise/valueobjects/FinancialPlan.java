package com.kinplatform.kin.enterprise.valueobjects;

/**
 * Plan financiero del proyecto empresarial (value object).
 *
 * <p>Representa el estudio financiero básico: CAPEX, OPEX, proyección de
 * ingresos a tres años, mes de punto de equilibrio, margen bruto y los tres
 * escenarios (optimista, base y pesimista). Cada escenario es un
 * {@link Scenario} inmutable con su propia proyección de ingresos y margen.
 * Producido por {@code FinancialPlanEngine}.</p>
 */
public record FinancialPlan(
    double capex,
    double opex,
    double revenueYear1,
    double revenueYear2,
    double revenueYear3,
    int breakEvenMonth,
    double grossMargin,
    Scenario optimistic,
    Scenario base,
    Scenario pessimistic
) {

    public FinancialPlan {
        ValueObjects.requireNonNegative(capex, "capex");
        ValueObjects.requireNonNegative(opex, "opex");
        ValueObjects.requireNonNegative(revenueYear1, "revenueYear1");
        ValueObjects.requireNonNegative(revenueYear2, "revenueYear2");
        ValueObjects.requireNonNegative(revenueYear3, "revenueYear3");
        ValueObjects.requireNonNegative(breakEvenMonth, "breakEvenMonth");
        ValueObjects.requireInRange(grossMargin, 0.0, 100.0, "grossMargin");
        if (optimistic == null) {
            throw new IllegalArgumentException("'optimistic' no puede ser null.");
        }
        if (base == null) {
            throw new IllegalArgumentException("'base' no puede ser null.");
        }
        if (pessimistic == null) {
            throw new IllegalArgumentException("'pessimistic' no puede ser null.");
        }
    }

    /**
     * Escenario financiero (value object): proyección de ingresos y margen.
     *
     * @param revenue ingresos proyectados (mayor o igual a 0)
     * @param margin  margen porcentual entre 0 y 100
     */
    public record Scenario(double revenue, double margin) {

        public Scenario {
            ValueObjects.requireNonNegative(revenue, "revenue");
            ValueObjects.requireInRange(margin, 0.0, 100.0, "margin");
        }

        public static Scenario of(double revenue, double margin) {
            return new Scenario(revenue, margin);
        }
    }

    /**
     * Crea un plan financiero vacío (todos los importes en cero).
     */
    public static FinancialPlan empty() {
        var emptyScenario = new Scenario(0.0, 0.0);
        return new FinancialPlan(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0,
            emptyScenario, emptyScenario, emptyScenario);
    }

    /**
     * Crea un plan financiero completo.
     */
    public static FinancialPlan of(double capex, double opex,
                                   double revenueYear1, double revenueYear2, double revenueYear3,
                                   int breakEvenMonth, double grossMargin,
                                   Scenario optimistic, Scenario base, Scenario pessimistic) {
        return new FinancialPlan(capex, opex, revenueYear1, revenueYear2, revenueYear3,
            breakEvenMonth, grossMargin, optimistic, base, pessimistic);
    }
}
