package com.kinplatform.kin.health.triage.share.api;

import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Contenido público de un triaje compartido (GET /health/triage/share/{token}).
 *
 * <p>Es el MÍNIMO de información que el paciente autorizó al compartir:
 * nombre del paciente (contexto clínico), fecha del triaje, síntomas,
 * condiciones con probabilidad/urgencia y la orientación. No expone email,
 * userId, ni ningún otro dato del paciente.</p>
 */
public record SharedTriageContent(
        String patientName,
        OffsetDateTime triageDate,
        List<String> symptoms,
        List<SharedCondition> conditions,
        String disclaimer) {

    public record SharedCondition(
            String name,
            String description,
            double probability,
            String severity,
            String urgency,
            String recommendation) {}

    public static SharedTriageContent from(
            TriageConsultation consultation, String patientName) {
        List<SharedCondition> conditions = consultation.results().stream()
                .map(SharedTriageContent::toCondition)
                .toList();
        return new SharedTriageContent(
                patientName,
                consultation.createdAt(),
                consultation.symptoms(),
                conditions,
                "Informe de apoyo a la decisión. No sustituye el diagnóstico de un profesional de la salud.");
    }

    private static SharedCondition toCondition(TriageConditionResult r) {
        return new SharedCondition(
                r.name(),
                r.description(),
                r.probability(),
                severityName(r.severity()),
                urgencyName(r.urgency()),
                r.recommendation());
    }

    private static String severityName(Severity severity) {
        return severity == null ? null : severity.name();
    }

    private static String urgencyName(Urgency urgency) {
        return urgency == null ? null : urgency.name();
    }
}
