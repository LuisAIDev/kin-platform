package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.DiagnosisRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        DiagnosisRequest request = new DiagnosisRequest(
                "J18.9",
                "PRINCIPAL",
                "CONFIRMED",
                "Examen físico y Rx tórax",
                LocalDate.now(),
                null
        );

        Set<ConstraintViolation<DiagnosisRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void invalidCie10Code_fails() {
        DiagnosisRequest request = new DiagnosisRequest(
                "INVALID",
                "PRINCIPAL",
                "CONFIRMED",
                null,
                null,
                null
        );

        Set<ConstraintViolation<DiagnosisRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("cie10Code");
    }

    @Test
    void blankCie10Code_fails() {
        DiagnosisRequest request = new DiagnosisRequest(
                "",
                "PRINCIPAL",
                "CONFIRMED",
                null,
                null,
                null
        );

        Set<ConstraintViolation<DiagnosisRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(2);
        assertThat(violations).extracting("propertyPath").extracting(Object::toString).containsExactlyInAnyOrder("cie10Code", "cie10Code");
    }

    @Test
    void invalidDiagnosisType_fails() {
        DiagnosisRequest request = new DiagnosisRequest(
                "J18.9",
                "INVALID_TYPE",
                "CONFIRMED",
                null,
                null,
                null
        );

        Set<ConstraintViolation<DiagnosisRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("diagnosisType");
    }

    @Test
    void invalidCertainty_fails() {
        DiagnosisRequest request = new DiagnosisRequest(
                "J18.9",
                "PRINCIPAL",
                "INVALID_CERTAINTY",
                null,
                null,
                null
        );

        Set<ConstraintViolation<DiagnosisRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("certainty");
    }

    @Test
    void supportedByTooLong_fails() {
        DiagnosisRequest request = new DiagnosisRequest(
                "J18.9",
                "PRINCIPAL",
                "CONFIRMED",
                "a".repeat(501),
                null,
                null
        );

        Set<ConstraintViolation<DiagnosisRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("supportedBy");
    }
}