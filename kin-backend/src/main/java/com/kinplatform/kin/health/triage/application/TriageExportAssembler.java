package com.kinplatform.kin.health.triage.application;

import com.kinplatform.kin.export.model.ExportBlock;
import com.kinplatform.kin.export.model.ExportDocument;
import com.kinplatform.kin.export.model.ExportFormat;
import com.kinplatform.kin.export.model.ExportSection;
import com.kinplatform.kin.export.renderer.ExportRendererFactory;
import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TriageExportAssembler {

    private static final Logger log = LoggerFactory.getLogger(TriageExportAssembler.class);

    private final ExportRendererFactory rendererFactory;
    private final DateTimeFormatter datetimeFormatter;

    public TriageExportAssembler(ExportRendererFactory rendererFactory) {
        this.rendererFactory = rendererFactory;
        this.datetimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    }

    @PostConstruct
    public void init() {
        log.info("TriageExportAssembler: PDF renderer ready via OpenPDF");
    }

    public byte[] toPdf(TriageExportDocument doc) {
        ExportDocument exportDoc = buildExportDocument(doc);
        return rendererFactory.rendererFor(ExportFormat.PDF).render(exportDoc);
    }

    private ExportDocument buildExportDocument(TriageExportDocument doc) {
        String title = "Informe de Triaje KIN";
        String subtitle = doc.patientName() != null ? doc.patientName() : "Paciente";

        List<ExportSection> sections = List.of(
                buildTriageSection(doc),
                buildAdvisorySection(),
                buildMetadataSection(doc)
        );

        return new ExportDocument(title, subtitle, doc.generatedAt(), sections);
    }

    private ExportSection buildTriageSection(TriageExportDocument doc) {
        ExportBlock symptoms = ExportBlock.paragraph("Motivo de consulta / Síntomas reportados: " + valueOr(doc.symptoms()));
        ExportBlock results = ExportBlock.paragraph("Resultado del triaje: " + valueOr(doc.results()));
        ExportBlock recommendation = ExportBlock.paragraph("Orientación / Recomendación: " + valueOr(doc.recommendation()));
        return ExportSection.of("Datos del triaje", List.of(symptoms, results, recommendation));
    }

    private ExportSection buildAdvisorySection() {
        ExportBlock advisory = ExportBlock.highlight(
                "Este informe no constituye un diagnóstico médico. Es una herramienta de apoyo informativa. "
                        + "Siempre consulta a un profesional de la salud para decisiones clínicas.");
        return ExportSection.of("Advertencia", List.of(advisory));
    }

    private ExportSection buildMetadataSection(TriageExportDocument doc) {
        ExportBlock code = ExportBlock.bold("Código único del informe: " + valueOr(doc.triageId() != null ? doc.triageId().toString() : "—"));
        ExportBlock generated = ExportBlock.bold(
                "Fecha de generación: " + (doc.generatedAt() != null ? datetimeFormatter.format(doc.generatedAt()) : "—"));
        ExportBlock kin = ExportBlock.bold("Generado por: " + valueOr(doc.kinIdentifier()));
        return ExportSection.of("Información del informe", List.of(code, generated, kin));
    }

    private String valueOr(String value) {
        return value != null && !value.isEmpty() ? value : "—";
    }
}
