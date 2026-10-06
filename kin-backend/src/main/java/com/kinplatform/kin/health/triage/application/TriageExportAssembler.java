package com.kinplatform.kin.health.triage.application;

import com.kinplatform.platform.export.model.ExportBlock;
import com.kinplatform.platform.export.model.ExportDocument;
import com.kinplatform.platform.export.model.ExportFormat;
import com.kinplatform.platform.export.model.ExportSection;
import com.kinplatform.platform.export.renderer.ExportRendererFactory;
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

    public TriageExportAssembler() {
        this.rendererFactory = new ExportRendererFactory();
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
                buildDisclaimerSection(),
                buildTriageSection(doc),
                buildAdvisorySection(),
                buildMetadataSection(doc)
        );

        return new ExportDocument(title, subtitle, doc.generatedAt(), sections);
    }

    private ExportSection buildDisclaimerSection() {
        String disclaimerTitle = "Triaje procesado por KIN Medical.";
        String disclaimerBody =
                "Las decisiones clínicas las toma un motor de reglas fijas que aplica guías "
                        + "médicas verificables. Trabajamos con datos reales de fuentes públicas y "
                        + "confiables: la Organización Mundial de la Salud (OMS), clasificaciones "
                        + "internacionales de enfermedades (CIE-10), rangos de referencia de "
                        + "laboratorios y protocolos clínicos.\n\n"
                        + "Qué significa esto? Que cada vez que analizamos tus síntomas, aplicamos "
                        + "las mismas reglas médicas validadas, sin improvisaciones ni variaciones. "
                        + "La inteligencia artificial solo se encarga de redactar el resultado en "
                        + "palabras sencillas para ti.\n\n"
                        + "Este triaje es de apoyo informativo y no sustituye la evaluación ni el "
                        + "diagnóstico de un profesional de la salud. Siempre consulta a tu médico.";

        ExportBlock titleBlock = ExportBlock.paragraph(disclaimerTitle);
        ExportBlock bodyBlock = ExportBlock.paragraph(disclaimerBody);
        return ExportSection.of("Procesamiento", List.of(titleBlock, bodyBlock));
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

