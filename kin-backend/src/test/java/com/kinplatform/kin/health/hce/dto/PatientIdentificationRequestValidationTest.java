package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.PatientIdentificationRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PatientIdentificationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        PatientIdentificationRequest request = new PatientIdentificationRequest(
                "CC",
                "1234567890",
                "O+",
                "CONTRIBUTIVO"
        );

        Set<ConstraintViolation<PatientIdentificationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankDocumentType_fails() {
        PatientIdentificationRequest request = new PatientIdentificationRequest(
                "",
                "1234567890",
                "O+",
                "CONTRIBUTIVO"
        );

        Set<ConstraintViolation<PatientIdentificationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(2);
        assertThat(violations).extracting("propertyPath").extracting(Object::toString).containsExactlyInAnyOrder("documentType", "documentType");
    }

    @Test
    void invalidDocumentType_fails() {
        PatientIdentificationRequest request = new PatientIdentificationRequest(
                "INVALID",
                "1234567890",
                "O+",
                "CONTRIBUTIVO"
        );

        Set<ConstraintViolation<PatientIdentificationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("documentType");
    }

    @Test
    void blankDocumentNumber_fails() {
        PatientIdentificationRequest request = new PatientIdentificationRequest(
                "CC",
                "",
                "O+",
                "CONTRIBUTIVO"
        );

        Set<ConstraintViolation<PatientIdentificationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("documentNumber");
    }

    @Test
    void invalidRhFactor_fails() {
        PatientIdentificationRequest request = new PatientIdentificationRequest(
                "CC",
                "1234567890",
                "INVALID",
                "CONTRIBUTIVO"
        );

        Set<ConstraintViolation<PatientIdentificationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("rhFactor");
    }

    @Test
    void invalidRegimen_fails() {
        PatientIdentificationRequest request = new PatientIdentificationRequest(
                "CC",
                "1234567890",
                "O+",
                "INVALID_REGIMEN"
        );

        Set<ConstraintViolation<PatientIdentificationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("regimen");
    }
}