package com.kinplatform.platform.export.model;

import java.util.List;

/**
 * Tabla neutral de un {@link ExportDocument}: fila de cabecera y filas de datos.
 *
 * <p>Value object inmutable. No se reutiliza el historial del chat como fuente;
 * las tablas se construyen a partir de datos estructurados del proyecto.</p>
 *
 * @param header encabezados de columna (nunca {@code null})
 * @param rows   filas de datos (nunca {@code null}); cada fila con el mismo
 *               número de columnas que {@code header}
 */
public record ExportTable(List<String> header, List<List<String>> rows) {

    public ExportTable {
        header = header == null ? List.of() : List.copyOf(header);
        rows = rows == null
                ? List.of()
                : rows.stream()
                        .map(row -> row == null ? List.<String>of() : List.copyOf(row))
                        .toList();
    }

    public int columnCount() {
        return header.size();
    }

    public boolean isEmpty() {
        return header.isEmpty() && rows.isEmpty();
    }

    public static ExportTable of(List<String> header, List<List<String>> rows) {
        return new ExportTable(header, rows);
    }
}

