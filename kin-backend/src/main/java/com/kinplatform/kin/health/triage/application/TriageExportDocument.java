package com.kinplatform.kin.health.triage.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TriageExportDocument(

        UUID triageId,
        UUID patientId,
        String patientName,
        OffsetDateTime triageDate,
        String symptoms,
        String results,
        String recommendation,
        OffsetDateTime generatedAt,
        String kinIdentifier

) {
    public static TriageExportDocument of(
            UUID triageId,
            UUID patientId,
            String patientName,
            OffsetDateTime triageDate,
            String symptoms,
            String results,
            String recommendation) {
        return new TriageExportDocument(
                triageId,
                patientId,
                patientName,
                triageDate,
                symptoms,
                results,
                recommendation,
                OffsetDateTime.now(),
                "KIN Platform"
        );
    }
}