package com.kinplatform.kin.health.triage.adapter;

import java.util.List;

/**
 * Entrada estructurada del dataset de condiciones médicas (ADR-028, fase
 * profesional). Es el contrato JSON de la fuente externa (bundle offline o API
 * médica): una condición con su CIE-10, severidad/urgencia/recomendación y los
 * síntomas asociados con peso y bandera de obligatorio.
 */
public class HealthCatalogEntry {

    private String name;
    private String description;
    private String icdCode;
    private String severity;
    private String urgency;
    private String recommendation;
    private List<SymptomRef> symptoms;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcdCode() {
        return icdCode;
    }

    public void setIcdCode(String icdCode) {
        this.icdCode = icdCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public List<SymptomRef> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(List<SymptomRef> symptoms) {
        this.symptoms = symptoms;
    }

    /** Referencia a un síntoma dentro de una entrada de condición. */
    public static class SymptomRef {
        private String name;
        private double weight;
        private boolean required;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}
