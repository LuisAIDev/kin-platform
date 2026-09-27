package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.UpdateEncounterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateEncounterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        UpdateEncounterRequest request = new UpdateEncounterRequest(
                "Dolor abdominal actualizado",
                "INPATIENT",
                "CLOSED"
        );

        Set<ConstraintViolation<UpdateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullStatus_fails() {
        UpdateEncounterRequest request = new UpdateEncounterRequest(
                "Dolor abdominal",
                "INPATIENT",
                null
        );

        Set<ConstraintViolation<UpdateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("status");
    }

    @Test
    void invalidStatus_fails() {
        UpdateEncounterRequest request = new UpdateEncounterRequest(
                "Dolor abdominal",
                "INPATIENT",
                "INVALID_STATUS"
        );

        Set<ConstraintViolation<UpdateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("status");
    }

    @Test
    void invalidEncounterType_fails() {
        UpdateEncounterRequest request = new UpdateEncounterRequest(
                "Dolor abdominal",
                "WRONG_TYPE",
                "OPEN"
        );

        Set<ConstraintViolation<UpdateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("encounterType");
    }

    @Test
    void chiefComplaintTooLong_fails() {
        String longComplaint = "a".repeat(501);
        UpdateEncounterRequest request = new UpdateEncounterRequest(
                longComplaint,
                "INPATIENT",
                "OPEN"
        );

        Set<ConstraintViolation<UpdateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("chiefComplaint");
    }
}