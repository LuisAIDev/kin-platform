package com.kinplatform.kin.enterprise.ports;

import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;

/**
 * Puerto de renderizado de documentos (Fase 10).
 *
 * <p>Contrato hexagonal de salida: convierte un {@link DocumentArtifact}
 * (representación neutral) en un formato binario de salida (PDF en el
 * Milestone 3; Word/PowerPoint en versiones posteriores) sin acoplar el
 * dominio a ninguna librería de renderizado. La implementación
 * (p. ej. {@code PdfDocumentRenderer}) es un adaptador que se aportará en el
 * Milestone 3.</p>
 */
public interface DocumentRenderer {

    /**
     * Renderiza el artefacto de documento a su representación binaria.
     *
     * @param artifact artefacto de documento a renderizar
     * @return bytes del documento renderizado
     */
    byte[] render(DocumentArtifact artifact);
}
