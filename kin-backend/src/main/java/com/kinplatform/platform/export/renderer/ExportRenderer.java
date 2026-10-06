package com.kinplatform.platform.export.renderer;

import com.kinplatform.platform.export.model.ExportDocument;

/**
 * Puerto de renderizado de un {@link ExportDocument} a un formato binario/texto.
 *
 * <p>Implementaciones stateless y thread-safe: solo transforman el modelo
 * neutral en bytes del formato objetivo. El contenido proviene íntegramente del
 * modelo (Project/ConsultingReport/project_info), nunca del historial de chat.</p>
 */
public interface ExportRenderer {

    byte[] render(ExportDocument document);
}


