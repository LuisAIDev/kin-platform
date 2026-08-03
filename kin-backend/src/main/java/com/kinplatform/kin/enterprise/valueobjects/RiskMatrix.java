package com.kinplatform.kin.enterprise.valueobjects;

import java.util.List;

/**
 * Matriz de riesgos del proyecto empresarial (value object).
 *
 * <p>Representa la presentación matricial de los riesgos identificados. Cada
 * riesgo es un {@link Risk} que combina probabilidad e impacto, deriva su
 * severidad ({@link RiskSeverity}) y porta la mitigación, el propietario y su
 * estado de gestión ({@link RiskStatus}). Producida por
 * {@code RiskPlanEngine}.</p>
 */
public record RiskMatrix(List<Risk> risks) {

    public RiskMatrix {
        risks = ValueObjects.immutableNonNull(risks, "risks");
    }

    /**
     * Riesgo individual de la matriz (value object).
     *
     * @param probability probabilidad de ocurrencia entre 0 y 1
     * @param impact      impacto si ocurre, entre 0 y 1
     * @param severity    severidad derivada ({@link RiskSeverity})
     * @param mitigation  medida de mitigación (no vacía)
     * @param owner       propietario responsable (no vacío)
     * @param status      estado de gestión ({@link RiskStatus})
     */
    public record Risk(
        double probability,
        double impact,
        RiskSeverity severity,
        String mitigation,
        String owner,
        RiskStatus status
    ) {

        public Risk {
            ValueObjects.requireInRange(probability, 0.0, 1.0, "probability");
            ValueObjects.requireInRange(impact, 0.0, 1.0, "impact");
            if (severity == null) {
                throw new IllegalArgumentException("'severity' no puede ser null.");
            }
            ValueObjects.requireNotBlank(mitigation, "mitigation");
            ValueObjects.requireNotBlank(owner, "owner");
            if (status == null) {
                throw new IllegalArgumentException("'status' no puede ser null.");
            }
        }

        public static Risk of(double probability, double impact, RiskSeverity severity,
                              String mitigation, String owner, RiskStatus status) {
            return new Risk(probability, impact, severity, mitigation, owner, status);
        }
    }

    /**
     * Crea una matriz de riesgos vacía (sin riesgos).
     */
    public static RiskMatrix empty() {
        return new RiskMatrix(List.of());
    }

    /**
     * Crea una matriz de riesgos a partir de la lista de riesgos.
     */
    public static RiskMatrix of(List<Risk> risks) {
        return new RiskMatrix(risks);
    }
}
