package com.kinplatform.kin.enterprise.renderer;

import com.kinplatform.kin.enterprise.ports.DocumentRenderer;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * Renderizador DOCX (Fase 10, Milestone 2H).
 *
 * <p>Convierte un {@link DocumentArtifact} (representación neutral) en un
 * documento Word (OOXML) válido generado íntegramente con el JDK: el paquete
 * DOCX es un ZIP con las partes {@code [Content_Types].xml}, {@code _rels/.rels},
 * {@code word/document.xml} y {@code docProps/core.xml}. El contenido del
 * artefacto se embebe como párrafos de texto (con el título del tipo de
 * documento como encabezado) y los caracteres especiales se escapan para XML.</p>
 *
 * <p>Renderizador stateless, thread-safe y sin efectos secundarios: solo
 * transforma datos (el artefacto en bytes DOCX).</p>
 */
public final class DocxDocumentRenderer implements DocumentRenderer {

    private static final String CONTENT_TYPES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n"
        + "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n"
        + "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n"
        + "  <Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n"
        + "  <Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>\n"
        + "</Types>";

    private static final String ROOT_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
        + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n"
        + "  <Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>\n"
        + "</Relationships>";

    @Override
    public RenderFormat format() {
        return RenderFormat.DOCX;
    }

    @Override
    public byte[] render(DocumentArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact no puede ser null");
        }
        List<String> lines = new ArrayList<>();
        lines.add(RendererText.title(artifact.type()));
        lines.add("");
        lines.addAll(RendererText.lines(artifact.content()));
        return buildDocx(artifact, lines);
    }

    private byte[] buildDocx(DocumentArtifact artifact, List<String> lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            RendererText.writeZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES);
            RendererText.writeZipEntry(zip, "_rels/.rels", ROOT_RELS);
            RendererText.writeZipEntry(zip, "word/document.xml", documentXml(lines));
            RendererText.writeZipEntry(zip, "docProps/core.xml", corePropertiesXml(artifact));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el DOCX", e);
        }
        return out.toByteArray();
    }

    private String documentXml(List<String> lines) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            .append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n")
            .append("  <w:body>\n");
        boolean first = true;
        for (String line : lines) {
            if (first) {
                xml.append("    <w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"36\"/></w:rPr><w:t>")
                    .append(RendererText.xmlEscape(line)).append("</w:t></w:r></w:p>\n");
                first = false;
            } else {
                xml.append("    <w:p><w:r><w:t xml:space=\"preserve\">")
                    .append(RendererText.xmlEscape(line)).append("</w:t></w:r></w:p>\n");
            }
        }
        xml.append("    <w:sectPr/>\n")
            .append("  </w:body>\n")
            .append("</w:document>");
        return xml.toString();
    }

    private String corePropertiesXml(DocumentArtifact artifact) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" "
            + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
            + "  <dc:title>" + RendererText.xmlEscape(RendererText.title(artifact.type())) + "</dc:title>\n"
            + "  <dc:creator>KIN Platform</dc:creator>\n"
            + "  <cp:revision>1</cp:revision>\n"
            + "</cp:coreProperties>";
    }
}
