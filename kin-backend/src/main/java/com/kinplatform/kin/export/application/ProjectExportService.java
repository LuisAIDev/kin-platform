package com.kinplatform.kin.export.application;

import com.kinplatform.kin.export.model.ExportFormat;
import com.kinplatform.kin.export.model.ExportMode;
import java.util.UUID;

/**
 * Puerto de exportación de proyectos (frontera de aplicación).
 *
 * <p>Verifica siempre la propiedad del proyecto: un usuario solo puede exportar
 * sus propios proyectos. No se crea almacenamiento permanente del documento
 * generado (se sirve en memoria).</p>
 */
public interface ProjectExportService {

    /**
     * Genera el documento exportado del proyecto en el formato solicitado.
     *
     * @param userId            usuario autenticado (dueño obligatorio del proyecto)
     * @param projectId         proyecto a exportar
     * @param format            formato de salida
     * @param mode              modo (completo o resumen)
     * @param templateDocumentId documento de referencia (estructura) del mismo
     *                           proyecto; {@code null} para exportación normal
     * @return bytes del documento y nombre de archivo
     * @throws org.springframework.web.server.ResponseStatusException 404 si el
     *         proyecto o el documento no existen o pertenecen a otro usuario;
     *         400 si el documento no puede usarse como plantilla
     */
    ExportResult export(UUID userId, UUID projectId, ExportFormat format, ExportMode mode, UUID templateDocumentId);

    /**
     * Opciones de exportación del proyecto (solo lectura).
     */
    ExportOptions options(UUID userId, UUID projectId);
}
