package com.kinplatform.kin.enterprise.renderer;

import com.kinplatform.kin.enterprise.ports.DocumentRenderer;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Renderizador PDF (Fase 10, Milestone 2H).
 *
 * <p>Convierte un {@link DocumentArtifact} (representación neutral) en un
 * documento PDF válido generado íntegramente con el JDK (sin dependencias
 * externas): páginas A4 con una sola página, fuente base Helvetica y el
 * contenido del artefacto embebido como líneas de texto. El contenido se
 * normaliza a Latin-1 (PDFDocEncoding), preservando los caracteres acentuados
 * del español y sustituyendo los no representables por {@code ?}.</p>
 *
 * <p>Renderizador stateless, thread-safe y sin efectos secundarios: solo
 * transforma datos (el artefacto en bytes PDF).</p>
 */
public final class PdfDocumentRenderer implements DocumentRenderer {

    private static final int LINE_WIDTH = 90;
    private static final int LINE_LEADING = 18;
    private static final int START_Y = 760;
    private static final int MARGIN_X = 72;

    @Override
    public RenderFormat format() {
        return RenderFormat.PDF;
    }

    @Override
    public byte[] render(DocumentArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact no puede ser null");
        }
        List<String> lines = new ArrayList<>();
        lines.add(RendererText.title(artifact.type()));
        lines.add("");
        for (String line : RendererText.lines(artifact.content())) {
            lines.addAll(RendererText.wrap(line, LINE_WIDTH));
        }
        return buildPdf(lines);
    }

    // ------------------------------------------------------------------
    // Construcción del PDF
    // ------------------------------------------------------------------

    /**
     * Construye un PDF de una página con el conjunto de líneas dado.
     *
     * <p>Estructura: cabecera, cinco objetos (catálogo, páginas, página,
     * flujo de contenido y fuente), tabla {@code xref} con desplazamientos
     * exactos y tráiler. El flujo de contenido se escribe sin compresión para
     * que el texto sea inspeccionable en bytes.</p>
     */
    private byte[] buildPdf(List<String> lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        writeBytes(out, "%PDF-1.4\n");

        offsets.add(out.size());
        writeBytes(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets.add(out.size());
        writeBytes(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        offsets.add(out.size());
        writeBytes(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
            + "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");

        byte[] stream = contentStream(lines);
        offsets.add(out.size());
        writeBytes(out, "4 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
        out.writeBytes(stream);
        writeBytes(out, "\nendstream\nendobj\n");

        offsets.add(out.size());
        writeBytes(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        long xrefOffset = out.size();
        writeBytes(out, "xref\n0 6\n0000000000 65535 f \n");
        for (int offset : offsets) {
            writeBytes(out, String.format("%010d 00000 n \n", offset));
        }
        writeBytes(out, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n");

        return out.toByteArray();
    }

    /**
     * Flujo de contenido del PDF: operador de texto con una línea por renglón
     * y avance de línea constante.
     */
    private byte[] contentStream(List<String> lines) {
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 12 Tf\n").append(MARGIN_X).append(' ').append(START_Y).append(" Td\n");
        for (String line : lines) {
            stream.append('(').append(pdfEscape(line)).append(") Tj\n0 -")
                .append(LINE_LEADING).append(" Td\n");
        }
        stream.append("ET\n");
        return stream.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * Escapa una línea para una cadena PDF y la normaliza a Latin-1: escapa
     * {@code \}, {@code (} y {@code )}, sustituye los caracteres de control por
     * espacio y los caracteres no representables en Latin-1 por {@code ?}.
     */
    private String pdfEscape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\').append(c);
            } else if (c < 32) {
                sb.append(' ');
            } else if (c <= 0xFF) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private void writeBytes(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
    }
}
