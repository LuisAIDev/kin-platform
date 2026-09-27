package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.SurgicalHistoryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SurgicalHistoryRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        SurgicalHistoryRequest request = new SurgicalHistoryRequest(
                "890201",
                LocalDate.of(2020, 5, 15),
                "Apendicectomía laparoscópica",
                "Apendicitis aguda",
                2,
                "GENERAL",
                "Hospital Central"
        );

        Set<ConstraintViolation<SurgicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankProcedureCupsCode_fails() {
        SurgicalHistoryRequest request = new SurgicalHistoryRequest(
                "",
                LocalDate.of(2020, 5, 15),
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<SurgicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("procedureCupsCode");
    }

    @Test
    void procedureCupsCodeTooLong_fails() {
        SurgicalHistoryRequest request = new SurgicalHistoryRequest(
                "a".repeat(21),
                LocalDate.of(2020, 5, 15),
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<SurgicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("procedureCupsCode");
    }

    @Test
    void nullSurgeryDate_fails() {
        SurgicalHistoryRequest request = new SurgicalHistoryRequest(
                "890201",
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<SurgicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("surgeryDate");
    }

    @Test
    void futureSurgeryDate_fails() {
        SurgicalHistoryRequest request = new SurgicalHistoryRequest(
                "890201",
                LocalDate.now().plusDays(1),
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<SurgicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("surgeryDate");
    }

    @Test
    void asaClassificationTooLow_fails() {
        SurgicalHistoryRequest request = new SurgicalHistoryRequest(
                "890201",
                LocalDate.of(2020, 5, 15),
                null,
                null,
                0,
                null,
                null
        );

        Set<ConstraintViolation<SurgicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("asaClassification");
    }

    @Test
    void asaClassificationTooHigh_fails() {
        SurgicalHistoryRequest request = new SurgicalHistoryRequest(
                "890201",
                LocalDate.of(2020, 5, 15),
                null,
                null,
                7,
                null,
                null
        );

        Set<ConstraintViolation<SurgicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("asaClassification");
    }

    @Test
    void invalidAnesthesiaType_fails() {
        SurgicalHistoryRequest request = new SurgicalHistoryRequest(
                "890201",
                LocalDate.of(2020, 5, 15),
                null,
                null,
                2,
                "INVALID_TYPE",
                null
        );

        Set<ConstraintViolation<SurgicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("anesthesiaType");
    }

    @Test
    void institutionTooLong_fails() {
        SurgicalHistoryRequest request = new SurgicalHistoryRequest(
                "890201",
                LocalDate.of(2020, 5, 15),
                null,
                null,
                2,
                "GENERAL",
                "a".repeat(201)
        );

        Set<ConstraintViolation<SurgicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("institution");
    }
}