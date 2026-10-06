package com.kinplatform.platform.export.intent;

import com.kinplatform.platform.export.model.ExportFormat;
import java.util.UUID;

/**
 * Acción aditiva de exportación adjunta a una respuesta del chat.
 *
 * <p>Cuando el usuario pide descargar su proyecto, el chat puede devolver esta
 * acción para que el frontend muestre una tarjeta de descarga directa. El campo
 * es opcional: {@code null} en las respuestas sin intención de exportación
 * (compatibilidad con clientes existentes).</p>
 *
 * @param type                  tipo de acción (siempre {@code EXPORT_PROJECT})
 * @param format                formato de salida detectado
 * @param templateDocumentId    documento de referencia (estructura) si se pidió
 *                              usar uno; {@code null} en caso contrario
 * @param templateDocumentName  nombre del documento de referencia (solo UI)
 */
public record ExportAction(String type, ExportFormat format, UUID templateDocumentId, String templateDocumentName) {

    public static final String TYPE_EXPORT_PROJECT = "EXPORT_PROJECT";

    public ExportAction {
        type = type == null ? TYPE_EXPORT_PROJECT : type;
        format = format == null ? ExportFormat.DOCX : format;
        templateDocumentName = templateDocumentName == null ? "" : templateDocumentName;
    }

    public static ExportAction of(ExportFormat format, UUID templateDocumentId, String templateDocumentName) {
        return new ExportAction(TYPE_EXPORT_PROJECT, format, templateDocumentId, templateDocumentName);
    }
}


