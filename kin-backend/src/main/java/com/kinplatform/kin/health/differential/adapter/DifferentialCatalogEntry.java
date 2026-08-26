package com.kinplatform.kin.health.differential.adapter;

import java.util.List;

/**
 * Dataset estructurado del diagnóstico diferencial (ADR-029).
 *
 * <p>Contrato JSON de la fuente externa (bundle offline o API médica): factores
 * de riesgo y pruebas recomendadas referenciadas por el nombre canónico de la
 * condición (resuelto contra el catálogo de triaje al aplicar la actualización).</p>
 */
public class DifferentialCatalogEntry {

    private List<RiskFactorRef> riskFactors;
    private List<TestRef> recommendedTests;

    public List<RiskFactorRef> getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(List<RiskFactorRef> riskFactors) {
        this.riskFactors = riskFactors;
    }

    public List<TestRef> getRecommendedTests() {
        return recommendedTests;
    }

    public void setRecommendedTests(List<TestRef> recommendedTests) {
        this.recommendedTests = recommendedTests;
    }

    /** Referencia a un factor de riesgo. */
    public static class RiskFactorRef {
        private String condition;
        private String factor;
        private double weight;
        private String description;

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getFactor() {
            return factor;
        }

        public void setFactor(String factor) {
            this.factor = factor;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /** Referencia a una prueba recomendada. */
    public static class TestRef {
        private String condition;
        private String test;
        private String description;

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
