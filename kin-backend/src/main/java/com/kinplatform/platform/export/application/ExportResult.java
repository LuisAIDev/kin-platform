package com.kinplatform.platform.export.application;

import com.kinplatform.platform.export.model.ExportFormat;

/**
 * Resultado de una exportación: bytes del documento y nombre del archivo.
 *
 * @param format   formato exportado
 * @param bytes    contenido binario/texto del documento
 * @param filename nombre sugerido para la descarga (sin exponer secretos)
 */
public record ExportResult(ExportFormat format, byte[] bytes, String filename) {}


