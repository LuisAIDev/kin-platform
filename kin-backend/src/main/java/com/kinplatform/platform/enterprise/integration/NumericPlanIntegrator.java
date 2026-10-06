package com.kinplatform.platform.enterprise.integration;

import com.kinplatform.platform.enterprise.valueobjects.MarketPlan;
import java.util.OptionalDouble;

/**
 * Integrador numérico Enterprise (FASE 10B). Puro, determinista y sin
 * dependencias: enriquece el {@code MarketPlan} con los datos estructurados del
 * usuario (TAM/SAM/SOM/growthRate) y expone cálculos financieros derivados
 * (ingresos, margen de contribución y punto de equilibrio en unidades).
 *
 * <p>Reglas: un dato desconocido NUNCA se convierte en 0; los cálculos se
 * realizan solo cuando existen todos los datos esenciales (en otro caso
 * devuelven {@link OptionalDouble#empty()}, que la capa de integración traduce
 * a NOT_AVAILABLE). Con entrada vacía el integrador es identidad (no-op):
 * Enterprise se comporta exactamente como antes.</p>
 */
public final class NumericPlanIntegrator {

    private NumericPlanIntegrator() {}

    /**
     * Enriquece el plan de mercado con los valores estructurados disponibles.
     * Solo sobrescribe un campo cuando el valor es numérico y no negativo;
     * en otro caso conserva el del plan original.
     */
    public static MarketPlan integrateMarketPlan(MarketPlan base, EnterpriseSupplementalInput input) {
        if (base == null || input == null || input.isEmpty()) {
            return base;
        }
        double tam = valueOr(base.tam(), input.tam());
        double sam = valueOr(base.sam(), input.sam());
        double som = valueOr(base.som(), input.som());
        double growth = valueOr(base.growthRate(), input.growthRate());
        if (tam == base.tam() && sam == base.sam() && som == base.som() && growth == base.growthRate()) {
            return base;
        }
        return MarketPlan.of(
                tam,
                sam,
                som,
                growth,
                base.competitors(),
                base.channels(),
                base.entryBarriers(),
                base.customerSegments(),
                base.confidence());
    }

    /** Ingresos = precio × unidades. Vacío si falta precio o unidades. */
    public static OptionalDouble ingresos(ResolvedValue unitPrice, ResolvedValue units) {
        OptionalDouble price = toDouble(unitPrice);
        OptionalDouble count = toDouble(units);
        if (price.isEmpty() || count.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(price.getAsDouble() * count.getAsDouble());
    }

    /** Margen de contribución = (precio − costoVariable) / precio. Vacío si falta algún dato. */
    public static OptionalDouble contributionMargin(ResolvedValue unitPrice, ResolvedValue variableCost) {
        OptionalDouble price = toDouble(unitPrice);
        OptionalDouble variable = toDouble(variableCost);
        if (price.isEmpty() || variable.isEmpty() || price.getAsDouble() <= 0.0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((price.getAsDouble() - variable.getAsDouble()) / price.getAsDouble());
    }

    /** Punto de equilibrio (unidades) = costosFijos / (precio − costoVariable). Vacío si falta algo. */
    public static OptionalDouble breakEvenUnits(
            ResolvedValue fixedCost, ResolvedValue unitPrice, ResolvedValue variableCost) {
        OptionalDouble fixed = toDouble(fixedCost);
        OptionalDouble price = toDouble(unitPrice);
        OptionalDouble variable = toDouble(variableCost);
        if (fixed.isEmpty() || price.isEmpty() || variable.isEmpty()) {
            return OptionalDouble.empty();
        }
        double contribution = price.getAsDouble() - variable.getAsDouble();
        if (contribution <= 0.0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(fixed.getAsDouble() / contribution);
    }

    private static double valueOr(double base, ResolvedValue value) {
        OptionalDouble parsed = toNonNegative(value);
        return parsed.isPresent() ? parsed.getAsDouble() : base;
    }

    private static OptionalDouble toNonNegative(ResolvedValue value) {
        OptionalDouble parsed = toDouble(value);
        if (parsed.isEmpty() || parsed.getAsDouble() < 0.0) {
            return OptionalDouble.empty();
        }
        return parsed;
    }

    private static OptionalDouble toDouble(ResolvedValue value) {
        if (value == null || value.value() == null || value.value().isBlank()) {
            return OptionalDouble.empty();
        }
        try {
            return OptionalDouble.of(Double.parseDouble(value.value().trim()));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }
}


