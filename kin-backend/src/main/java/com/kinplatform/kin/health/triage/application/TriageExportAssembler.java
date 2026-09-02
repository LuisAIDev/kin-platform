package com.kinplatform.kin.health.triage.application;

import com.kinplatform.kin.export.ExportFormat;
import com.kinplatform.kin.export.renderer.ExportRendererFactory;
import com.kinplatform.kin.export.model.ExportDocument;
import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TriageExportAssembler {

    private static final Logger log = LoggerFactory.getLogger(TriageExportAssembler.class);

    private final ExportRendererFactory rendererFactory;
    private final DateTimeFormatter dateFormatter;
    private final DateTimeFormatter datetimeFormatter;

    public TriageExportAssembler(ExportRendererFactory rendererFactory) {
        this.rendererFactory = rendererFactory;
        this.dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
        var sections = java.util.List.of(
                buildHeaderSection(doc),
                buildPatientSection(doc),
                buildTriageSection(doc),
                buildAdvisorySection(doc),
                buildMetadataSection(doc)
        );

        var content = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (var section : sections) {
            content.add(section);
        }

        return new ExportDocument(
                doc.patientName + " — Informe de Triaje",
                content
        );
    }

    private java.util.Map<String, Object> buildHeaderSection(TriageExportDocument doc) {
        var map = java.util.LinkedHashMap<String, Object>();
        map.put("type", "header");
        map.put("title", "INFORME DE TRIAJE KIN");
        map.put("code", doc.triageId != null ? doc.triageId.toString() : "—");
        map.put("generatedAt", doc.generatedAt != null ? doc.generatedAt.toString() : "—");
        return map;
    }

    private java.util.Map<String, Object> buildPatientSection(TriageExportDocument doc) {
        var map = java.util.LinkedHashMap<String, Object>();
        map.put("type", "patient-info");
        map.put("label", "Paciente");
        map.put("value", doc.patientName != null ? doc.patientName : "—");
        return map;
    }

    private java.util.Map<String, Object> buildTriageSection(TriageExportDocument doc) {
        var map = java.util.LinkedHashMap<String, Object>();
        map.put("type", "triage-info");
        var sb = new java.lang.StringBuilder();
        if (doc.symptoms != null && !doc.symptoms.isEmpty()) {
            sb.append("Motivo de consulta / Síntomas reportados:\\n");
            sb.append(doc.symptoms).append("\\n\\n");
        }
        if (doc.results != null && !doc.results.isEmpty()) {
            sb.append("Resultado del triaje:\\n");
            sb.append(doc.results).append("\\n\\n");
        }
        if (doc.recommendation != null && !doc.recommendation.isEmpty()) {
            sb.append("Orientación / Recomendación:\\n");
            sb.append(doc.recommendation).append("\\n");
        }
        map.put("content", sb.toString());
        return map;
    }

    private java.util.Map<String, Object> buildAdvisorySection(TriageExportDocument doc) {
        var map = java.util.LinkedHashMap<String, Object>();
        map.put("type", "advisory");
        map.put("content", "\\n\\nAdvertencia: Este informe no constituye un diagnóstico médico. Es una herramienta de apoyo informativa. Siempre consulta a un profesional de la salud para decisiones clínicas.");
        return map;
    }

    private java.util.Map<String, Object> buildMetadataSection(TriageExportDocument doc) {
        var map = java.util.LinkedHashMap<String, Object>();
        map.put("type", "metadata");
        map.put("label", "Información del informe");
        map.put("dateGenerated", doc.generatedAt != null ? dateFormatter.format(doc.generatedAt) : "—");
        map.put("kinIdentifier", doc.kinIdentifier);
        return map;
    }
}