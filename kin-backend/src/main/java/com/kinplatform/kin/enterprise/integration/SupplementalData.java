package com.kinplatform.kin.enterprise.integration;

import com.kinplatform.projectinfo.StructuredInfoSourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Datos suplementarios para Enterprise derivados de
 * {@code project_structured_info} (finanzas, mercado, impacto y riesgos), con
 * estado explícito por dato.
 *
 * <p>Reglas: un dato ausente se representa como {@code PENDING} (nunca como
 * {@code 0}); un dato real cero se conserva como {@code CONFIRMED} con valor
 * "0". Los cálculos incompletos devuelven {@code NOT_AVAILABLE} con la lista de
 * campos faltantes en lugar de inventar una cifra.</p>
 */
public final class SupplementalData {

    private static final String FINANZAS = "FINANZAS";
    private static final String MERCADO = "MERCADO";
    private static final String IMPACTO = "IMPACTO";
    private static final String RIESGOS = "RIESGOS";

    private final Map<String, ResolvedValue> financial;
    private final Map<String, ResolvedValue> market;
    private final Map<String, ResolvedValue> impact;
    private final Map<String, ResolvedValue> risk;
    private final BreakevenAssessment breakeven;

    private SupplementalData(
            Map<String, ResolvedValue> financial,
            Map<String, ResolvedValue> market,
            Map<String, ResolvedValue> impact,
            Map<String, ResolvedValue> risk,
            BreakevenAssessment breakeven) {
        this.financial = Map.copyOf(financial);
        this.market = Map.copyOf(market);
        this.impact = Map.copyOf(impact);
        this.risk = Map.copyOf(risk);
        this.breakeven = breakeven;
    }

    /** Construye los datos suplementarios a partir de la información estructurada. */
    public static SupplementalData from(List<StructuredDatum> data) {
        Map<String, ResolvedValue> financial = sectionValues(data, FINANZAS);
        Map<String, ResolvedValue> market = sectionValues(data, MERCADO);
        Map<String, ResolvedValue> impact = sectionValues(data, IMPACTO);
        Map<String, ResolvedValue> risk = sectionValues(data, RIESGOS);
        return new SupplementalData(financial, market, impact, risk, breakeven(financial));
    }

    public Map<String, ResolvedValue> financial() {
        return financial;
    }

    public Map<String, ResolvedValue> market() {
        return market;
    }

    public Map<String, ResolvedValue> impact() {
        return impact;
    }

    public Map<String, ResolvedValue> risk() {
        return risk;
    }

    public BreakevenAssessment breakeven() {
        return breakeven;
    }

    public ResolvedValue financial(String key) {
        return financial.getOrDefault(key, ResolvedValue.pending());
    }

    public ResolvedValue market(String key) {
        return market.getOrDefault(key, ResolvedValue.pending());
    }

    private static Map<String, ResolvedValue> sectionValues(List<StructuredDatum> data, String section) {
        Map<String, ResolvedValue> values = new LinkedHashMap<>();
        for (StructuredDatum datum : data) {
            if (datum.section().equalsIgnoreCase(section) && datum.hasValue()) {
                values.put(
                        datum.key().toLowerCase(),
                        ResolvedValue.of(datum.value(), datum.sourceType(), origin(datum.sourceType())));
            }
        }
        return values;
    }

    private static BreakevenAssessment breakeven(Map<String, ResolvedValue> financial) {
        List<String> missing = new ArrayList<>();
        Optional<Double> fixedCosts = number(financial, "costos_fijos", missing);
        Optional<Double> unitPrice = number(financial, "precio", missing);
        Optional<Double> variableCosts = number(financial, "costos_variables", missing);

        if (fixedCosts.isEmpty() || unitPrice.isEmpty() || variableCosts.isEmpty()) {
            return BreakevenAssessment.notCalculable(missing);
        }
        double contribution = unitPrice.get() - variableCosts.get();
        if (contribution <= 0.0) {
            return BreakevenAssessment.notCalculable(List.of("margen de contribución no positivo"));
        }
        long units = Math.max(0L, Math.round(fixedCosts.get() / contribution));
        return BreakevenAssessment.calculated(String.valueOf(units));
    }

    private static Optional<Double> number(Map<String, ResolvedValue> financial, String key, List<String> missing) {
        ResolvedValue value = financial.get(key);
        if (value == null || value.value() == null) {
            missing.add(key);
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(value.value().trim()));
        } catch (NumberFormatException e) {
            missing.add(key);
            return Optional.empty();
        }
    }

    private static String origin(StructuredInfoSourceType sourceType) {
        return switch (sourceType) {
            case USER_INPUT -> "Usuario";
            case IMPORTED_DOCUMENT -> "Documento";
            case CALCULATED -> "Calculado";
            case ESTIMATED -> "Estimado";
            case AI_SUGGESTED -> "IA";
        };
    }

    /** Evaluación del punto de equilibrio (unidades) con estado explícito. */
    public record BreakevenAssessment(ResolvedValue value, List<String> missingFields) {

        public static BreakevenAssessment calculated(String units) {
            return new BreakevenAssessment(
                    ResolvedValue.of(units, StructuredInfoSourceType.CALCULATED, "Calculado"), List.of());
        }

        public static BreakevenAssessment notCalculable(List<String> missingFields) {
            return new BreakevenAssessment(ResolvedValue.notAvailable(), List.copyOf(missingFields));
        }

        public boolean calculable() {
            return value.state() == DataState.CALCULATED;
        }
    }
}
