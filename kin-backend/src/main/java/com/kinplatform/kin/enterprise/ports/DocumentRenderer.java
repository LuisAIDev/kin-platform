package com.kinplatform.kin.enterprise.ports;

import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

/**
 * Puerto de renderizado de documentos (Fase 10).
 *
 * <p>Contrato hexagonal de salida: convierte un {@link DocumentArtifact}
 * (representación neutral) en la representación binaria de un formato de
 * salida concreto (PDF en el Milestone 3; Word/PowerPoint en versiones
 * posteriores) sin acoplar el dominio a ninguna librería de renderizado.</p>
 *
 * <p>Cada implementación se vincula a UN formato mediante {@link #format()}:
 * habrá un renderizador PDF, uno DOCX y uno PPTX (adaptadores que se aportarán
 * en el Milestone 3). Un consumidor selecciona el renderizador adecuado
 * comparando {@link RenderFormat} sin conocer la implementación concreta.</p>
 */
public interface DocumentRenderer {

    /**
     * Formato de salida que produce esta implementación (PDF, DOCX o PPTX).
     */
    RenderFormat format();

    /**
     * Renderiza el artefacto de documento a su representación binaria.
     *
     * @param artifact artefacto de documento a renderizar
     * @return bytes del documento renderizado en el formato declarado
     */
    byte[] render(DocumentArtifact artifact);
}
