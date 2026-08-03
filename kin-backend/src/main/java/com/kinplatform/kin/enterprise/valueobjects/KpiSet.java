package com.kinplatform.kin.enterprise.valueobjects;

import java.util.List;

/**
 * Conjunto de KPIs del proyecto empresarial (value object).
 *
 * <p>Representa los KPIs agrupados por fase del ciclo de vida del cliente y
 * financiera: adquisición, activación, retención, ingresos y financieros.
 * Cada KPI es un {@link Kpi} con nombre, objetivo, valor actual, fórmula y
 * frecuencia. Producido por {@code KpiEngine}.</p>
 */
public record KpiSet(
    List<Kpi> acquisition,
    List<Kpi> activation,
    List<Kpi> retention,
    List<Kpi> revenue,
    List<Kpi> financial
) {

    public KpiSet {
        acquisition = ValueObjects.immutableNonNull(acquisition, "acquisition");
        activation = ValueObjects.immutableNonNull(activation, "activation");
        retention = ValueObjects.immutableNonNull(retention, "retention");
        revenue = ValueObjects.immutableNonNull(revenue, "revenue");
        financial = ValueObjects.immutableNonNull(financial, "financial");
    }

    /**
     * KPI individual (value object).
     *
     * @param name         nombre del indicador (no vacío)
     * @param target       objetivo a alcanzar
     * @param currentValue valor actual del indicador
     * @param formula      fórmula de cálculo (no vacía)
     * @param frequency    frecuencia de medición (no vacía)
     */
    public record Kpi(String name, double target, double currentValue,
                      String formula, String frequency) {

        public Kpi {
            ValueObjects.requireNotBlank(name, "name");
            ValueObjects.requireNotBlank(formula, "formula");
            ValueObjects.requireNotBlank(frequency, "frequency");
        }

        public static Kpi of(String name, double target, double currentValue,
                             String formula, String frequency) {
            return new Kpi(name, target, currentValue, formula, frequency);
        }
    }

    /**
     * Crea un conjunto de KPIs vacío (las cinco fases quedan vacías).
     */
    public static KpiSet empty() {
        return new KpiSet(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Crea un conjunto de KPIs a partir de las cinco fases.
     */
    public static KpiSet of(List<Kpi> acquisition, List<Kpi> activation, List<Kpi> retention,
                            List<Kpi> revenue, List<Kpi> financial) {
        return new KpiSet(acquisition, activation, retention, revenue, financial);
    }
}
