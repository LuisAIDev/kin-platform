package com.kinplatform.kin.ai.prompt;

import com.kinplatform.kin.reporting.report.model.ReportSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;

/**
 * Formatea una {@link ReportSection} a texto legible (Markdown ligero).
 *
 * <p>Implementaciones <strong>stateless</strong> y puras: no calculan, no deciden,
 * solo transforman la sección ya calculada en representación textual para el prompt.</p>
 *
 * @param <T> tipo concreto de sección (subtipo de {@link ReportSection})
 */
public interface SectionFormatter<T extends ReportSection> {

    /**
     * Formatea la sección a texto.
     *
     * @param section sección a formatear (nunca {@code null})
     * @return representación en Markdown ligero (nunca {@code null})
     */
    String format(T section);

    /**
     * Tipo de sección que este formatter maneja.
     * Usado por {@link ReportPromptBuilder} para resolver el formatter correcto.
     */
    ReportSectionKind kind();
}