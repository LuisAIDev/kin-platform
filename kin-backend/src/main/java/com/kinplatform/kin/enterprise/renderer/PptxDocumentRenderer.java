package com.kinplatform.kin.enterprise.renderer;

import com.kinplatform.kin.enterprise.ports.DocumentRenderer;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * Renderizador PPTX (Fase 10, Milestone 2H).
 *
 * <p>Convierte un {@link DocumentArtifact} (representación neutral) en una
 * presentación PowerPoint (OOXML) válida generada íntegramente con el JDK: el
 * paquete PPTX es un ZIP con las partes de presentación, diapositiva, diseño,
 * patrón y tema. El contenido del artefacto se embebe como una diapositiva con
 * el título del tipo de documento y un bloque de texto con las líneas del
 * contenido; los caracteres especiales se escapan para XML.</p>
 *
 * <p>Renderizador stateless, thread-safe y sin efectos secundarios: solo
 * transforma datos (el artefacto en bytes PPTX).</p>
 */
public final class PptxDocumentRenderer implements DocumentRenderer {

    private static final String NS_P = "http://schemas.openxmlformats.org/presentationml/2006/main";
    private static final String NS_A = "http://schemas.openxmlformats.org/drawingml/2006/main";
    private static final String NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    @Override
    public RenderFormat format() {
        return RenderFormat.PPTX;
    }

    @Override
    public byte[] render(DocumentArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact no puede ser null");
        }
        String title = RendererText.title(artifact.type());
        return buildPptx(artifact, title, RendererText.lines(artifact.content()));
    }

    private byte[] buildPptx(DocumentArtifact artifact, String title, List<String> lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            RendererText.writeZipEntry(zip, "[Content_Types].xml", contentTypesXml());
            RendererText.writeZipEntry(zip, "_rels/.rels", rootRelsXml());
            RendererText.writeZipEntry(zip, "ppt/presentation.xml", presentationXml());
            RendererText.writeZipEntry(zip, "ppt/_rels/presentation.xml.rels", presentationRelsXml());
            RendererText.writeZipEntry(zip, "ppt/slides/slide1.xml", slideXml(title, lines));
            RendererText.writeZipEntry(zip, "ppt/slides/_rels/slide1.xml.rels", slideRelsXml());
            RendererText.writeZipEntry(zip, "ppt/slideLayouts/slideLayout1.xml", slideLayoutXml());
            RendererText.writeZipEntry(zip, "ppt/slideLayouts/_rels/slideLayout1.xml.rels", slideLayoutRelsXml());
            RendererText.writeZipEntry(zip, "ppt/slideMasters/slideMaster1.xml", slideMasterXml());
            RendererText.writeZipEntry(zip, "ppt/slideMasters/_rels/slideMaster1.xml.rels", slideMasterRelsXml());
            RendererText.writeZipEntry(zip, "ppt/theme/theme1.xml", themeXml());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PPTX", e);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------
    // Partes estáticas del paquete
    // ------------------------------------------------------------------

    private String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n"
            + "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n"
            + "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n"
            + "  <Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/>\n"
            + "  <Override PartName=\"/ppt/slides/slide1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>\n"
            + "  <Override PartName=\"/ppt/slideLayouts/slideLayout1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml\"/>\n"
            + "  <Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml\"/>\n"
            + "  <Override PartName=\"/ppt/theme/theme1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.theme+xml\"/>\n"
            + "</Types>";
    }

    private String rootRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
            + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"ppt/presentation.xml\"/>\n"
            + "</Relationships>";
    }

    private String presentationXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<p:presentation xmlns:a=\"" + NS_A + "\" xmlns:r=\"" + NS_R + "\" xmlns:p=\"" + NS_P + "\">\n"
            + "  <p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rId2\"/></p:sldMasterIdLst>\n"
            + "  <p:sldIdLst><p:sldId id=\"256\" r:id=\"rId1\"/></p:sldIdLst>\n"
            + "  <p:sldSz cx=\"9144000\" cy=\"6858000\"/>\n"
            + "  <p:notesSz cx=\"6858000\" cy=\"9144000\"/>\n"
            + "</p:presentation>";
    }

    private String presentationRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
            + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" Target=\"slides/slide1.xml\"/>\n"
            + "  <Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster\" Target=\"slideMasters/slideMaster1.xml\"/>\n"
            + "</Relationships>";
    }

    private String slideRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
            + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>\n"
            + "</Relationships>";
    }

    private String slideLayoutXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<p:sldLayout xmlns:a=\"" + NS_A + "\" xmlns:r=\"" + NS_R + "\" xmlns:p=\"" + NS_P
            + "\" type=\"blank\" preserve=\"1\">\n"
            + "  <p:cSld name=\"Blank\"><p:spTree>\n"
            + "    <p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>\n"
            + "    <p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>\n"
            + "  </p:spTree></p:cSld>\n"
            + "  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>\n"
            + "</p:sldLayout>";
    }

    private String slideLayoutRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
            + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster\" Target=\"../slideMasters/slideMaster1.xml\"/>\n"
            + "</Relationships>";
    }

    private String slideMasterXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<p:sldMaster xmlns:a=\"" + NS_A + "\" xmlns:r=\"" + NS_R + "\" xmlns:p=\"" + NS_P + "\">\n"
            + "  <p:cSld><p:spTree>\n"
            + "    <p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>\n"
            + "    <p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>\n"
            + "  </p:spTree></p:cSld>\n"
            + "  <p:clrMap bg1=\"lt1\" tx1=\"dk1\" bg2=\"lt2\" tx2=\"dk2\" accent1=\"accent1\" accent2=\"accent2\" "
            + "accent3=\"accent3\" accent4=\"accent4\" accent5=\"accent5\" accent6=\"accent6\" hlink=\"hlink\" folHlink=\"folHlink\"/>\n"
            + "  <p:sldLayoutIdLst><p:sldLayoutId id=\"2147483649\" r:id=\"rId1\"/></p:sldLayoutIdLst>\n"
            + "  <p:txStyles>\n"
            + "    <p:titleStyle><a:lvl1pPr><a:defRPr sz=\"4400\" kern=\"1200\"/></a:lvl1pPr></p:titleStyle>\n"
            + "    <p:bodyStyle><a:lvl1pPr/><a:lvl2pPr/><a:lvl3pPr/><a:lvl4pPr/><a:lvl5pPr/><a:lvl6pPr/><a:lvl7pPr/><a:lvl8pPr/><a:lvl9pPr/></p:bodyStyle>\n"
            + "    <p:otherStyle/>\n"
            + "  </p:txStyles>\n"
            + "</p:sldMaster>";
    }

    private String slideMasterRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
            + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>\n"
            + "  <Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme\" Target=\"../theme/theme1.xml\"/>\n"
            + "</Relationships>";
    }

    private String themeXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<a:theme xmlns:a=\"" + NS_A + "\" name=\"KIN\">\n"
            + "  <a:themeElements>\n"
            + "    <a:clrScheme name=\"KIN\">\n"
            + "      <a:dk1><a:srgbClr val=\"000000\"/></a:dk1>\n"
            + "      <a:lt1><a:srgbClr val=\"FFFFFF\"/></a:lt1>\n"
            + "      <a:dk2><a:srgbClr val=\"1F497D\"/></a:dk2>\n"
            + "      <a:lt2><a:srgbClr val=\"EEECE1\"/></a:lt2>\n"
            + "      <a:accent1><a:srgbClr val=\"4F81BD\"/></a:accent1>\n"
            + "      <a:accent2><a:srgbClr val=\"C0504D\"/></a:accent2>\n"
            + "      <a:accent3><a:srgbClr val=\"9BBB59\"/></a:accent3>\n"
            + "      <a:accent4><a:srgbClr val=\"8064A2\"/></a:accent4>\n"
            + "      <a:accent5><a:srgbClr val=\"4BACC6\"/></a:accent5>\n"
            + "      <a:accent6><a:srgbClr val=\"F79646\"/></a:accent6>\n"
            + "      <a:hlink><a:srgbClr val=\"0000FF\"/></a:hlink>\n"
            + "      <a:folHlink><a:srgbClr val=\"800080\"/></a:folHlink>\n"
            + "    </a:clrScheme>\n"
            + "    <a:fontScheme name=\"KIN\">\n"
            + "      <a:majorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:majorFont>\n"
            + "      <a:minorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:minorFont>\n"
            + "    </a:fontScheme>\n"
            + "    <a:fmtScheme name=\"KIN\">\n"
            + "      <a:fillStyleLst>\n"
            + "        <a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>\n"
            + "        <a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>\n"
            + "        <a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>\n"
            + "      </a:fillStyleLst>\n"
            + "      <a:lnStyleLst>\n"
            + "        <a:ln w=\"9525\" cap=\"flat\" cmpd=\"sng\" algn=\"ctr\"><a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill><a:prstDash val=\"solid\"/></a:ln>\n"
            + "        <a:ln w=\"25400\" cap=\"flat\" cmpd=\"sng\" algn=\"ctr\"><a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill><a:prstDash val=\"solid\"/></a:ln>\n"
            + "        <a:ln w=\"38100\" cap=\"flat\" cmpd=\"sng\" algn=\"ctr\"><a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill><a:prstDash val=\"solid\"/></a:ln>\n"
            + "      </a:lnStyleLst>\n"
            + "      <a:effectStyleLst>\n"
            + "        <a:effectStyle><a:effectLst/></a:effectStyle>\n"
            + "        <a:effectStyle><a:effectLst/></a:effectStyle>\n"
            + "        <a:effectStyle><a:effectLst/></a:effectStyle>\n"
            + "      </a:effectStyleLst>\n"
            + "      <a:bgFillStyleLst>\n"
            + "        <a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>\n"
            + "        <a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>\n"
            + "        <a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>\n"
            + "      </a:bgFillStyleLst>\n"
            + "    </a:fmtScheme>\n"
            + "  </a:themeElements>\n"
            + "  <a:objectDefaults/>\n"
            + "  <a:extraClrSchemeLst/>\n"
            + "</a:theme>";
    }

    // ------------------------------------------------------------------
    // Parte dinámica: la diapositiva
    // ------------------------------------------------------------------

    private String slideXml(String title, List<String> lines) {
        StringBuilder body = new StringBuilder();
        for (String line : lines) {
            if (line.isBlank()) {
                body.append("        <a:p/>\n");
            } else {
                body.append("        <a:p><a:r><a:t>")
                    .append(RendererText.xmlEscape(line))
                    .append("</a:t></a:r></a:p>\n");
            }
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<p:sld xmlns:a=\"" + NS_A + "\" xmlns:r=\"" + NS_R + "\" xmlns:p=\"" + NS_P + "\">\n"
            + "  <p:cSld><p:spTree>\n"
            + "    <p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>\n"
            + "    <p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>\n"
            + "    <p:sp>\n"
            + "      <p:nvSpPr><p:cNvPr id=\"2\" name=\"Title\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>\n"
            + "      <p:spPr><a:xfrm><a:off x=\"457200\" y=\"274638\"/><a:ext cx=\"8229600\" cy=\"1219200\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></p:spPr>\n"
            + "      <p:txBody><a:bodyPr wrap=\"none\"/><a:lstStyle/><a:p><a:r><a:rPr lang=\"es-ES\" sz=\"4400\" b=\"1\"/><a:t>"
            + RendererText.xmlEscape(title) + "</a:t></a:r></a:p></p:txBody>\n"
            + "    </p:sp>\n"
            + "    <p:sp>\n"
            + "      <p:nvSpPr><p:cNvPr id=\"3\" name=\"Content\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>\n"
            + "      <p:spPr><a:xfrm><a:off x=\"457200\" y=\"1758950\"/><a:ext cx=\"8229600\" cy=\"4800600\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></p:spPr>\n"
            + "      <p:txBody><a:bodyPr wrap=\"square\" rtlCol=\"0\"><a:normAutofit/></a:bodyPr><a:lstStyle/>\n"
            + body
            + "      </p:txBody>\n"
            + "    </p:sp>\n"
            + "  </p:spTree></p:cSld>\n"
            + "  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>\n"
            + "</p:sld>";
    }
}
