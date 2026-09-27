package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.CreateEncounterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateEncounterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        CreateEncounterRequest request = new CreateEncounterRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Dolor abdominal agudo",
                "EMERGENCY"
        );

        Set<ConstraintViolation<CreateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullPatientId_fails() {
        CreateEncounterRequest request = new CreateEncounterRequest(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Dolor abdominal",
                "EMERGENCY"
        );

        Set<ConstraintViolation<CreateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("patientId");
    }

    @Test
    void invalidEncounterType_fails() {
        CreateEncounterRequest request = new CreateEncounterRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Dolor abdominal",
                "INVALID_TYPE"
        );

        Set<ConstraintViolation<CreateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("encounterType");
    }

    @Test
    void blankChiefComplaint_fails() {
        CreateEncounterRequest request = new CreateEncounterRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "   ",
                "EMERGENCY"
        );

        Set<ConstraintViolation<CreateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("chiefComplaint");
    }

    @Test
    void chiefComplaintTooLong_fails() {
        String longComplaint = "a".repeat(501);
        CreateEncounterRequest request = new CreateEncounterRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                longComplaint,
                "EMERGENCY"
        );

        Set<ConstraintViolation<CreateEncounterRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("chiefComplaint");
    }
}