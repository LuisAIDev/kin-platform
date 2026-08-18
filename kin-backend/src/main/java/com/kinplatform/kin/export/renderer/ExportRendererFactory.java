package com.kinplatform.kin.export.renderer;

import com.kinplatform.kin.export.model.ExportFormat;
import java.util.EnumMap;
import java.util.Map;

/**
 * Fábrica de renderizadores de exportación por {@link ExportFormat}.
 *
 * <p>Stateless y thread-safe. DOCX usa Apache POI XWPF, PDF usa OpenPDF y
 * Markdown es texto plano; todos consumen el mismo {@code ExportDocument}.</p>
 */
public final class ExportRendererFactory {

    private final Map<ExportFormat, ExportRenderer> byFormat;

    public ExportRendererFactory() {
        this(new EnumMap<>(Map.of(
                ExportFormat.DOCX, new DocxExportRenderer(),
                ExportFormat.PDF, new PdfExportRenderer(),
                ExportFormat.MARKDOWN, new MarkdownExportRenderer())));
    }

    public ExportRendererFactory(Map<ExportFormat, ExportRenderer> byFormat) {
        if (byFormat == null || byFormat.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos un renderer");
        }
        this.byFormat = Map.copyOf(byFormat);
    }

    public ExportRenderer rendererFor(ExportFormat format) {
        ExportRenderer renderer = byFormat.get(format);
        if (renderer == null) {
            throw new IllegalArgumentException("No hay renderer para formato " + format);
        }
        return renderer;
    }
}
