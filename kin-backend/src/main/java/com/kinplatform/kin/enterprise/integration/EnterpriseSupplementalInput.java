package com.kinplatform.kin.enterprise.integration;

import java.util.List;
import java.util.Map;

/**
 * Entrada numérica suplementaria para los motores Enterprise (FASE 10B).
 *
 * <p>Porta los datos numéricos estructurados (TAM/SAM/SOM, growthRate, precio,
 * unidades, costos, inversión y margen) con su {@link ResolvedValue} (valor,
 * origen y estado). Un dato ausente se representa como {@code PENDING} (nunca
 * como {@code 0}); un dato no numérico como {@code NOT_AVAILABLE}.</p>
 *
 * <p>NO reemplaza los records de entrada de los motores: es una entrada
 * independiente consumida por {@link NumericPlanIntegrator}.</p>
 */
public record EnterpriseSupplementalInput(
        ResolvedValue tam,
        ResolvedValue sam,
        ResolvedValue som,
        ResolvedValue growthRate,
        ResolvedValue unitPrice,
        ResolvedValue units,
        ResolvedValue variableCost,
        ResolvedValue fixedCost,
        ResolvedValue initialInvestment,
        ResolvedValue margin) {

    private static final List<String> GROWTH_KEYS =
            List.of("growth_rate", "growthrate", "tasa_crecimiento", "crecimiento");
    private static final List<String> UNITS_KEYS = List.of("ventas_estimadas", "ventas", "ventas_anuales", "unidades");

    /** Entrada sin datos: el integrador se comporta como identidad (no-op). */
    public static EnterpriseSupplementalInput empty() {
        return new EnterpriseSupplementalInput(
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending());
    }

    /** Construye la entrada tipada a partir de los datos suplementarios resueltos. */
    public static EnterpriseSupplementalInput from(SupplementalData supplemental) {
        return new EnterpriseSupplementalInput(
                numeric(supplemental.market(), "tam"),
                numeric(supplemental.market(), "sam"),
                numeric(supplemental.market(), "som"),
                firstNumeric(supplemental.market(), GROWTH_KEYS),
                numeric(supplemental.financial(), "precio"),
                firstNumeric(supplemental.financial(), UNITS_KEYS),
                numeric(supplemental.financial(), "costos_variables"),
                numeric(supplemental.financial(), "costos_fijos"),
                numeric(supplemental.financial(), "inversion_inicial"),
                numeric(supplemental.financial(), "margen"));
    }

    /** True si ningún campo aporta un valor numérico utilizable. */
    public boolean isEmpty() {
        return allPendingOrUnavailable(
                tam, sam, som, growthRate, unitPrice, units, variableCost, fixedCost, initialInvestment, margin);
    }

    private static boolean allPendingOrUnavailable(ResolvedValue... values) {
        for (ResolvedValue value : values) {
            if (value != null && value.value() != null && isNumeric(value.value())) {
                return false;
            }
        }
        return true;
    }

    private static ResolvedValue numeric(Map<String, ResolvedValue> values, String key) {
        ResolvedValue value = values.get(key);
        if (value == null || value.value() == null || value.value().isBlank()) {
            return ResolvedValue.pending();
        }
        return isNumeric(value.value()) ? value : ResolvedValue.notAvailable();
    }

    private static ResolvedValue firstNumeric(Map<String, ResolvedValue> values, List<String> keys) {
        for (String key : keys) {
            ResolvedValue value = numeric(values, key);
            if (value.state() != DataState.PENDING) {
                return value;
            }
        }
        return ResolvedValue.pending();
    }

    private static boolean isNumeric(String value) {
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
