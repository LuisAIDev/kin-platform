package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.MedicalOrderRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalOrderRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_medication_passes() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "MEDICATION",
                "ROUTINE",
                null,
                "Ibuprofeno",
                "400",
                "mg",
                "ORAL",
                "Cada 8 horas",
                5
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void happyPath_procedure_withCupsCode_passes() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "PROCEDURE",
                "URGENT",
                "890201",
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullOrderType_fails() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                null,
                "ROUTINE",
                null,
                "Ibuprofeno",
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("orderType");
    }

    @Test
    void invalidOrderType_fails() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "INVALID_TYPE",
                "ROUTINE",
                null,
                "Ibuprofeno",
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("orderType");
    }

    @Test
    void nullPriority_fails() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "MEDICATION",
                null,
                null,
                "Ibuprofeno",
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("priority");
    }

    @Test
    void cupsCodeTooLong_fails() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "PROCEDURE",
                "ROUTINE",
                "123456789012345678901",
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("cupsCode");
    }

    @Test
    void procedureWithoutCupsCode_fails() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "PROCEDURE",
                "ROUTINE",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("cupsCode es obligatorio");
    }

    @Test
    void labExamWithoutCupsCode_fails() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "LAB_EXAM",
                "ROUTINE",
                "",
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("cupsCode es obligatorio");
    }

    @Test
    void imagingWithoutCupsCode_fails() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "IMAGING",
                "ROUTINE",
                "   ",
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("cupsCode es obligatorio");
    }

    @Test
    void medicationWithoutCupsCode_passes() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "MEDICATION",
                "ROUTINE",
                null,
                "Ibuprofeno",
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void drugNameTooLong_fails() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "MEDICATION",
                "ROUTINE",
                null,
                "a".repeat(201),
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("drugName");
    }

    @Test
    void doseTooLong_fails() {
        MedicalOrderRequest request = new MedicalOrderRequest(
                "MEDICATION",
                "ROUTINE",
                null,
                "Ibuprofeno",
                "a".repeat(201),
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<MedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("dose");
    }
}