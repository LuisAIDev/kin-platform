package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.DischargeSummaryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DischargeSummaryRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        DischargeSummaryRequest request = new DischargeSummaryRequest(
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2024, 1, 15),
                "I21.9",
                "Paciente ingresado por dolor torácico...",
                "Control cardiológico en 15 días",
                "STABLE"
        );

        Set<ConstraintViolation<DischargeSummaryRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullAdmissionDate_fails() {
        DischargeSummaryRequest request = new DischargeSummaryRequest(
                null,
                LocalDate.of(2024, 1, 15),
                "I21.9",
                "Resumen clínico",
                null,
                null
        );

        Set<ConstraintViolation<DischargeSummaryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("admissionDate");
    }

    @Test
    void dischargeDateBeforeAdmission_fails() {
        DischargeSummaryRequest request = new DischargeSummaryRequest(
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 1, 10),
                "I21.9",
                "Resumen clínico",
                null,
                null
        );

        Set<ConstraintViolation<DischargeSummaryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("dischargeDate debe ser posterior");
    }

    @Test
    void dischargeDateEqualAdmission_passes() {
        DischargeSummaryRequest request = new DischargeSummaryRequest(
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2024, 1, 10),
                "I21.9",
                "Resumen clínico",
                null,
                null
        );

        Set<ConstraintViolation<DischargeSummaryRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void invalidCie10Code_fails() {
        DischargeSummaryRequest request = new DischargeSummaryRequest(
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2024, 1, 15),
                "INVALID",
                "Resumen clínico",
                null,
                null
        );

        Set<ConstraintViolation<DischargeSummaryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("dischargeDiagnosisCie10");
    }

    @Test
    void clinicalSummaryTooLong_fails() {
        DischargeSummaryRequest request = new DischargeSummaryRequest(
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2024, 1, 15),
                "I21.9",
                "a".repeat(5001),
                null,
                null
        );

        Set<ConstraintViolation<DischargeSummaryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("clinicalSummary");
    }

    @Test
    void invalidDischargeCondition_fails() {
        DischargeSummaryRequest request = new DischargeSummaryRequest(
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2024, 1, 15),
                "I21.9",
                "Resumen clínico",
                null,
                "INVALID_CONDITION"
        );

        Set<ConstraintViolation<DischargeSummaryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("dischargeCondition");
    }
}