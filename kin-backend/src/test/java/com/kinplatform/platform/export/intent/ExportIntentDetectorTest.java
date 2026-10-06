package com.kinplatform.platform.export.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.kinplatform.platform.export.model.ExportFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * El detector identifica intención de exportación con reglas deterministas y
 * nunca actúa sin una señal clara (compatibilidad con el chat).
 */
class ExportIntentDetectorTest {

    private final ExportIntentDetector detector = new ExportIntentDetector();
    private final UUID documentId = UUID.randomUUID();

    @Test
    void detectaDescargaEnWordConFormatoDocxPorDefecto() {
        ExportAction action = detector.detect("Descárgame el proyecto en Word", null);
        assertNotNull(action);
        assertEquals("EXPORT_PROJECT", action.type());
        assertEquals(ExportFormat.DOCX, action.format());
        assertNull(action.templateDocumentId());
    }

    @Test
    void detectaFormatoPdf() {
        ExportAction action = detector.detect("Exporta mi proyecto en PDF", null);
        assertNotNull(action);
        assertEquals(ExportFormat.PDF, action.format());
    }

    @Test
    void detectaFormatoMarkdown() {
        ExportAction action = detector.detect("Descargar proyecto en markdown", null);
        assertNotNull(action);
        assertEquals(ExportFormat.MARKDOWN, action.format());
    }

    @Test
    void detectaPlantillaConDocumentoDeReferencia() {
        ExportAction action = detector.detect("Descárgame este proyecto usando el documento que subí", documentId);
        assertNotNull(action);
        assertEquals(ExportFormat.DOCX, action.format());
        assertEquals(documentId, action.templateDocumentId());
    }

    @Test
    void sinIntencionDevuelveNull() {
        assertNull(detector.detect("Hola, ¿cómo estás?", documentId));
        assertNull(detector.detect("Cuéntame sobre el mercado", documentId));
    }

    @Test
    void mensajeVacioDevuelveNull() {
        assertNull(detector.detect("   ", documentId));
        assertNull(detector.detect(null, documentId));
    }

    @Test
    void sinMencionDeDocumentoNoAdjuntaPlantilla() {
        ExportAction action = detector.detect("Quiero descargar mi proyecto", documentId);
        assertNotNull(action);
        assertNull(action.templateDocumentId());
    }
}


