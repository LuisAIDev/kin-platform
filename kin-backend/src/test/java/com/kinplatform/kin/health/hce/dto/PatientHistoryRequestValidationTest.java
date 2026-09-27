package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.PatientHistoryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PatientHistoryRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        PatientHistoryRequest request = new PatientHistoryRequest(
                "ALLERGY",
                "Alergia a penicilina"
        );

        Set<ConstraintViolation<PatientHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullHistoryType_fails() {
        PatientHistoryRequest request = new PatientHistoryRequest(
                null,
                "Alergia a penicilina"
        );

        Set<ConstraintViolation<PatientHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("historyType");
    }

    @Test
    void invalidHistoryType_fails() {
        PatientHistoryRequest request = new PatientHistoryRequest(
                "INVALID_TYPE",
                "Alergia a penicilina"
        );

        Set<ConstraintViolation<PatientHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("historyType");
    }

    @Test
    void blankDescription_fails() {
        PatientHistoryRequest request = new PatientHistoryRequest(
                "ALLERGY",
                ""
        );

        Set<ConstraintViolation<PatientHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");
    }
}