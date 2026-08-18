package com.kinplatform.kin.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.export.model.ExportBlockType;
import com.kinplatform.kin.export.model.ExportDocument;
import com.kinplatform.kin.export.model.ExportMode;
import com.kinplatform.kin.export.model.ExportSection;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.report.model.ExecutiveSummary;
import com.kinplatform.kin.reporting.report.model.FinancialSection;
import com.kinplatform.kin.reporting.report.model.InnovationSection;
import com.kinplatform.kin.reporting.report.model.MarketSection;
import com.kinplatform.kin.reporting.report.model.NextStepsSection;
import com.kinplatform.kin.reporting.report.model.OpportunitiesSection;
import com.kinplatform.kin.reporting.report.model.RecommendationsSection;
import com.kinplatform.kin.reporting.report.model.ReportMetadata;
import com.kinplatform.kin.reporting.report.model.RisksSection;
import com.kinplatform.kin.reporting.report.model.ScoresSection;
import com.kinplatform.kin.reporting.report.model.SourcesSection;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * El ensamblador construye el documento desde Project + ConsultingReport +
 * project_info. Regla: nunca inventa contenido y no usa el historial de chat.
 */
class ProjectExportAssemblerTest {

    private final ProjectExportAssembler assembler = new ProjectExportAssembler();

    @Test
    void identificaElProyectoConSusDatosReales() {
        var doc = assembler.assemble(input());
        var identification = section(doc, "Identificación del proyecto");

        assertEquals("CAFÉ MARTE 777", doc.title());
        assertTrue(contains(identification, "cafetería espacial"));
        assertTrue(contains(identification, "Gastronomía y Alimentos"));
        assertTrue(contains(identification, "DRAFT"));
        assertTrue(contains(identification, "77"));
    }

    @Test
    void proyectoCompletoIncluyeLasSeccionesDelReporte() {
        var doc = assembler.assemble(input());
        assertTrue(hasSection(doc, "Resumen Ejecutivo"));
        assertTrue(hasSection(doc, "Información: MERCADO"));
    }

    @Test
    void modoResumenSoloIncluyeIdentificacionYResumenEjecutivo() {
        var input = new ExportInput(
                "CAFÉ MARTE 777",
                "cafetería espacial",
                "Gastronomía y Alimentos",
                "DRAFT",
                BigDecimal.valueOf(77),
                "Resumen de KIN",
                null,
                null,
                report(),
                List.of(),
                ExportMode.SUMMARY);
        var doc = assembler.assemble(input);

        assertTrue(hasSection(doc, "Identificación del proyecto"));
        assertTrue(hasSection(doc, "Resumen Ejecutivo"));
        assertFalse(hasSection(doc, "Información: MERCADO"));
    }

    @Test
    void proyectoSinReporteNoInventaSeccionesDeReporte() {
        var input = new ExportInput(
                "CAFÉ MARTE 777",
                "cafetería espacial",
                "Gastronomía y Alimentos",
                "DRAFT",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                ExportMode.COMPLETE);
        var doc = assembler.assemble(input);

        assertTrue(hasSection(doc, "Identificación del proyecto"));
        assertFalse(hasSection(doc, "Resumen Ejecutivo"));
        assertFalse(hasSection(doc, "Métricas de Viabilidad"));
    }

    @Test
    void proyectoVacioNoGeneraContenidoInventado() {
        var input =
                new ExportInput("Sin datos", "", "", "", null, null, null, null, null, List.of(), ExportMode.COMPLETE);
        var doc = assembler.assemble(input);
        var identification = section(doc, "Identificación del proyecto");

        assertEquals(1, doc.sections().size());
        assertTrue(contains(identification, "Pendiente de información"));
        assertFalse(doc.toString().contains("historial"));
    }

    @Test
    void informacionEstructuradaSeMuestraComoTabla() {
        var doc = assembler.assemble(input());
        var market = section(doc, "Información: MERCADO");

        assertTrue(market.blocks().stream().anyMatch(b -> b.type() == ExportBlockType.TABLE));
        var table = market.blocks().stream()
                .filter(b -> b.type() == ExportBlockType.TABLE)
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("Campo", "Valor", "Fuente"), table.table().header());
        assertTrue(table.table().rows().stream().anyMatch(r -> r.contains("astronautas")));
    }

    @Test
    void historialIncompletoNoAfectaLaExportacion() {
        // El input NO contiene mensajes; el documento se construye igual.
        var doc = assembler.assemble(input());
        assertTrue(hasSection(doc, "Resumen Ejecutivo"));
        assertTrue(hasSection(doc, "Información: MERCADO"));
    }

    private ExportInput input() {
        return new ExportInput(
                "CAFÉ MARTE 777",
                "cafetería espacial orientada a astronautas",
                "Gastronomía y Alimentos",
                "DRAFT",
                BigDecimal.valueOf(77),
                "Resumen generado por KIN",
                null,
                null,
                report(),
                List.of(new StructuredInfo("MERCADO", "mercado_objetivo", "astronautas", "USER_INPUT")),
                ExportMode.COMPLETE);
    }

    private ConsultingReport report() {
        ExecutiveSummary summary = new ExecutiveSummary(
                "CAFÉ MARTE 777",
                "Gastronomía y Alimentos",
                77,
                100,
                "Viable",
                80.0,
                "Cafetería espacial con potencial.",
                List.of("Café", "Alimentos", "Espacio"));
        return new ConsultingReport(
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
    }

    private boolean hasSection(ExportDocument doc, String title) {
        return doc.sections().stream().anyMatch(s -> s.title().equals(title));
    }

    private ExportSection section(ExportDocument doc, String title) {
        return doc.sections().stream()
                .filter(s -> s.title().equals(title))
                .findFirst()
                .orElseThrow();
    }

    private boolean contains(ExportSection section, String text) {
        return section.toString().contains(text);
    }
}
