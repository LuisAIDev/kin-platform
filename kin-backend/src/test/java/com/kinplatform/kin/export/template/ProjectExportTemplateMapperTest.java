package com.kinplatform.kin.export.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.export.ExportInput;
import com.kinplatform.kin.export.ProjectExportAssembler;
import com.kinplatform.kin.export.StructuredInfo;
import com.kinplatform.kin.export.model.ExportBlockType;
import com.kinplatform.kin.export.model.ExportDocument;
import com.kinplatform.kin.export.model.ExportMode;
import com.kinplatform.kin.export.model.ExportSection;
import com.kinplatform.platform.reporting.report.model.ConsultingReport;
import com.kinplatform.platform.reporting.report.model.ExecutiveSummary;
import com.kinplatform.platform.reporting.report.model.FinancialSection;
import com.kinplatform.platform.reporting.report.model.InnovationSection;
import com.kinplatform.platform.reporting.report.model.MarketSection;
import com.kinplatform.platform.reporting.report.model.NextStepsSection;
import com.kinplatform.platform.reporting.report.model.OpportunitiesSection;
import com.kinplatform.platform.reporting.report.model.RecommendationsSection;
import com.kinplatform.platform.reporting.report.model.ReportMetadata;
import com.kinplatform.platform.reporting.report.model.RisksSection;
import com.kinplatform.platform.reporting.report.model.ScoresSection;
import com.kinplatform.platform.reporting.report.model.SourcesSection;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * El mapper llena la estructura de la plantilla con datos reales de KIN y marca
 * "Pendiente de información" las secciones sin datos equivalentes. Nunca inventa.
 */
class ProjectExportTemplateMapperTest {

    private final ProjectExportTemplateParser parser = new ProjectExportTemplateParser();
    private final ProjectExportTemplateMapper mapper = new ProjectExportTemplateMapper(new ProjectExportAssembler());

    @Test
    void respetaLaEstructuraYElOrdenDeLaPlantilla() {
        String reference = "1. INTRODUCCIÓN\n2. OBJETIVOS\n3. CONCLUSIONES";
        ExportTemplate template = parser.parse("Formato.docx", "text/plain", reference);

        ExportDocument doc = mapper.map(template, input());

        assertEquals("1. INTRODUCCIÓN", doc.sections().get(0).title());
        assertEquals("2. OBJETIVOS", doc.sections().get(1).title());
        assertEquals("3. CONCLUSIONES", doc.sections().get(2).title());
    }

    @Test
    void datosDelProyectoPueblanLaIntroduccion() {
        ExportTemplate template = parser.parse("F.docx", "text/plain", "1. INTRODUCCIÓN");
        ExportDocument doc = mapper.map(template, input());

        ExportSection intro = doc.sections().get(0);
        assertTrue(intro.toString().contains("cafetería espacial"));
        assertTrue(intro.toString().contains("CAFÉ MARTE 777"));
    }

    @Test
    void datosDeProjectInfoPueblanElPresupuesto() {
        ExportTemplate template = parser.parse("F.docx", "text/plain", "1. PRESUPUESTO");
        ExportDocument doc = mapper.map(template, input());

        ExportSection presupuesto = doc.sections().get(0);
        assertTrue(presupuesto.blocks().stream().anyMatch(b -> b.type() == ExportBlockType.TABLE));
        assertTrue(presupuesto.toString().contains("inversion_inicial"));
        assertTrue(presupuesto.toString().contains("50000"));
    }

    @Test
    void seccionSinDatosEquivalentesMuestraPendiente() {
        ExportTemplate template = parser.parse("F.docx", "text/plain", "1. Cronograma");
        ExportDocument doc = mapper.map(template, input());

        ExportSection cronograma = doc.sections().get(0);
        assertTrue(cronograma.toString().contains("Pendiente de información"));
    }

    @Test
    void noCopiaElContenidoDelDocumentoDeReferencia() {
        String reference = "1. INTRODUCCIÓN\n\nContenido literal inventado que pertenece a la plantilla.\n"
                + "2. METODOLOGÍA\n\nMetodología ficticia de la plantilla.";
        ExportTemplate template = parser.parse("Plantilla.docx", "text/plain", reference);

        ExportDocument doc = mapper.map(template, input());

        assertFalse(doc.toString().contains("Contenido literal inventado"));
        assertFalse(doc.toString().contains("Metodología ficticia"));
    }

    private ExportInput input() {
        ExecutiveSummary summary = new ExecutiveSummary(
                "CAFÉ MARTE 777",
                "Gastronomía y Alimentos",
                77,
                100,
                "Viable",
                80.0,
                "Cafetería espacial con potencial.",
                List.of("Café", "Alimentos"));
        ConsultingReport report = new ConsultingReport(
                UUID.randomUUID(),
                UUID.randomUUID(),
                summary,
                ScoresSection.empty(),
                RecommendationsSection.empty(),
                RisksSection.empty(),
                OpportunitiesSection.empty(),
                FinancialSection.empty(),
                MarketSection.empty(),
                InnovationSection.empty(),
                NextStepsSection.empty(),
                SourcesSection.empty(),
                ReportMetadata.empty());
        return new ExportInput(
                "CAFÉ MARTE 777",
                "cafetería espacial orientada a astronautas",
                "Gastronomía y Alimentos",
                "DRAFT",
                BigDecimal.valueOf(77),
                "Resumen generado por KIN",
                null,
                null,
                report,
                List.of(new StructuredInfo("FINANZAS", "inversion_inicial", "50000", "USER_INPUT")),
                ExportMode.COMPLETE);
    }
}

