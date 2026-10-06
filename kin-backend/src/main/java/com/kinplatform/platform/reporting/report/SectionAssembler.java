package com.kinplatform.platform.reporting.report;

import com.kinplatform.platform.reporting.report.model.ReportSection;

/**
 * Contrato de ensamblado de una sección del reporte: produce una sección
 * tipada a partir del {@link ReportInput}. Stateless.
 *
 * <p>No expone {@code sectionName()}: el nombre lo aporta la sección
 * producida (única fuente de verdad en {@link ReportSection}).</p>
 */
public interface SectionAssembler<T extends ReportSection> {

    T assemble(ReportInput input);
}


