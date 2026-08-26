package com.kinplatform.kin.health.triage.domain;

import java.util.List;

/**
 * Puerto de extracción de síntomas desde texto libre (ADR-028).
 *
 * <p>El dominio define el contrato; existen dos implementaciones: la
 * determinista por keywords ({@link KeywordSymptomExtractor}, dominio puro) y
 * la basada en NLP ({@code OpenNLPSymptomExtractor}, infraestructura) que
 * delega en la primera como fallback. Java decide la normalización; el NLP solo
 * amplía los candidatos.</p>
 */
public interface SymptomExtractor {

    /**
     * Devuelve los nombres canónicos de los síntomas del catálogo detectados en
     * el texto libre.
     *
     * @param catalog catálogo de conocimiento
     * @param text    texto libre (mensaje del usuario)
     * @return nombres canónicos de los síntomas detectados, en orden del catálogo
     */
    List<String> extract(TriageCatalog catalog, String text);
}
