package com.kinplatform.kin.export.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.export.model.ExportBlockType;
import org.junit.jupiter.api.Test;

/**
 * El parser es determinista y solo extrae estructura (nunca contenido).
 */
class ProjectExportTemplateParserTest {

    private final ProjectExportTemplateParser parser = new ProjectExportTemplateParser();

    @Test
    void detectaTituloYSeccionesEnOrden() {
        String text = "PORTADA\n\n1. INTRODUCCIÓN\n\n2. OBJETIVOS\n\n3. JUSTIFICACIÓN";
        ExportTemplate template = parser.parse("Formato.docx", "text/plain", text);

        assertEquals("PORTADA", template.title());
        assertEquals(3, template.sections().size());
        assertEquals("1. INTRODUCCIÓN", template.sections().get(0).title());
        assertEquals("2. OBJETIVOS", template.sections().get(1).title());
        assertEquals("3. JUSTIFICACIÓN", template.sections().get(2).title());
        assertEquals(1, template.sections().get(0).level());
    }

    @Test
    void detectaSubsecciones() {
        String text = "1. INTRODUCCIÓN\n1.1 Antecedentes\n1.2 Alcance";
        ExportTemplate template = parser.parse("doc.txt", "text/plain", text);

        assertEquals(3, template.sections().size());
        assertEquals(1, template.sections().get(0).level());
        assertEquals(2, template.sections().get(1).level());
        assertEquals(2, template.sections().get(2).level());
    }

    @Test
    void detectaListasComoSugerenciaEstructural() {
        String text = "1. OBJETIVOS\n- Objetivo A\n- Objetivo B";
        ExportTemplate template = parser.parse("doc.txt", "text/plain", text);

        var hints = template.sections().get(0).hints();
        assertEquals(1, hints.size());
        assertEquals(ExportBlockType.LIST, hints.get(0).type());
        assertEquals(2, hints.get(0).items().size());
    }

    @Test
    void detectaTablasCuandoSonConsistentes() {
        String text = "1. PRESUPUESTO\nCampo\tValor\nTotal\t1000\n";
        ExportTemplate template = parser.parse("doc.txt", "text/plain", text);

        var hints = template.sections().get(0).hints();
        assertEquals(1, hints.size());
        assertEquals(ExportBlockType.TABLE, hints.get(0).type());
        assertEquals(2, hints.get(0).table().columnCount());
        assertEquals(1, hints.get(0).table().rows().size());
    }

    @Test
    void noInventaContenidoNiParrafosComoEstructura() {
        String text = "1. INTRODUCCIÓN\n\nTexto plano de la introducción que NO debe ser estructura.\n\n"
                + "2. PRESUPUESTO\nCampo\tValor\nIncompleto\n";
        ExportTemplate template = parser.parse("doc.txt", "text/plain", text);

        assertTrue(template.sections().get(0).hints().isEmpty(), "los párrafos no se convierten en estructura");
        assertTrue(template.sections().get(1).hints().isEmpty(), "tabla inconsistente se degrada (no se fabrica)");
    }

    @Test
    void documentoVacioEsRechazado() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> parser.parse("doc.txt", "text/plain", "   \n  "));
    }
}
