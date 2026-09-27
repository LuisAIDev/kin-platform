package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.ClinicalAttachmentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalAttachmentRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        ClinicalAttachmentRequest request = new ClinicalAttachmentRequest(
                "LAB_RESULT",
                OffsetDateTime.now(),
                "storage/key123",
                "1234-5",
                "Glucosa en suero",
                "mg/dL",
                "70-100",
                "1.2.840.10008.1.2.4.1.1",
                "95",
                "Resultado dentro de rango normal"
        );

        Set<ConstraintViolation<ClinicalAttachmentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullAttachmentType_fails() {
        ClinicalAttachmentRequest request = new ClinicalAttachmentRequest(
                null,
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<ClinicalAttachmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("attachmentType");
    }

    @Test
    void invalidAttachmentType_fails() {
        ClinicalAttachmentRequest request = new ClinicalAttachmentRequest(
                "INVALID_TYPE",
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<ClinicalAttachmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("attachmentType");
    }

    @Test
    void nullPerformedAt_fails() {
        ClinicalAttachmentRequest request = new ClinicalAttachmentRequest(
                "LAB_RESULT",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<ClinicalAttachmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("performedAt");
    }

    @Test
    void storageKeyTooLong_fails() {
        ClinicalAttachmentRequest request = new ClinicalAttachmentRequest(
                "LAB_RESULT",
                OffsetDateTime.now(),
                "a".repeat(501),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<ClinicalAttachmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("storageKey");
    }

    @Test
    void loincCodeTooLong_fails() {
        ClinicalAttachmentRequest request = new ClinicalAttachmentRequest(
                "LAB_RESULT",
                OffsetDateTime.now(),
                "storage/key",
                "a".repeat(201),
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<ClinicalAttachmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("loincCode");
    }

    @Test
    void resultTextTooLong_fails() {
        ClinicalAttachmentRequest request = new ClinicalAttachmentRequest(
                "LAB_RESULT",
                OffsetDateTime.now(),
                "storage/key",
                null,
                null,
                null,
                null,
                null,
                null,
                "a".repeat(5001)
        );

        Set<ConstraintViolation<ClinicalAttachmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("resultText");
    }
}