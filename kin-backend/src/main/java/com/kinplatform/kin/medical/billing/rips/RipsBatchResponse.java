package com.kinplatform.kin.medical.billing.rips;

import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RipsBatchResponse(
        UUID id,
        UUID organizationId,
        UUID contractId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        int recordCount,
        int errorCount,
        List<ValidationErrorResponse> errors,
        Instant createdAt,
        Instant validatedAt) {
    public static RipsBatchResponse from(RipsBatch batch) {
        return new RipsBatchResponse(
                batch.getId(),
                batch.getOrganizationId(),
                batch.getContractId(),
                batch.getPeriodStart(),
                batch.getPeriodEnd(),
                batch.getStatus().name(),
                batch.getRecordCount(),
                batch.getErrorCount(),
                parseErrors(batch.getValidationErrors()),
                batch.getCreatedAt() != null ? batch.getCreatedAt().toInstant() : null,
                batch.getValidatedAt() != null ? batch.getValidatedAt().toInstant() : null);
    }

    private static List<ValidationErrorResponse> parseErrors(String validationErrorsJson) {
        if (validationErrorsJson == null || validationErrorsJson.isBlank()) {
            return List.of();
        }
        try {
            return List.of(new ValidationErrorResponse(validationErrorsJson));
        } catch (Exception e) {
            return List.of(new ValidationErrorResponse("Error parseando errores: " + e.getMessage()));
        }
    }
}
