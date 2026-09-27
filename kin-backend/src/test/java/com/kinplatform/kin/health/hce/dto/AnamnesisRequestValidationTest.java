package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.AnamnesisRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnamnesisRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        AnamnesisRequest request = new AnamnesisRequest(
                OffsetDateTime.now(),
                "Paciente refiere dolor abdominal de 2 horas de evolución",
                7
        );

        Set<ConstraintViolation<AnamnesisRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullOnsetDatetime_fails() {
        AnamnesisRequest request = new AnamnesisRequest(
                null,
                "Dolor abdominal",
                5
        );

        Set<ConstraintViolation<AnamnesisRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("onsetDatetime");
    }

    @Test
    void severitySelfReportedTooLow_fails() {
        AnamnesisRequest request = new AnamnesisRequest(
                OffsetDateTime.now(),
                "Dolor abdominal",
                0
        );

        Set<ConstraintViolation<AnamnesisRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("severitySelfReported");
    }

    @Test
    void severitySelfReportedTooHigh_fails() {
        AnamnesisRequest request = new AnamnesisRequest(
                OffsetDateTime.now(),
                "Dolor abdominal",
                11
        );

        Set<ConstraintViolation<AnamnesisRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("severitySelfReported");
    }

    @Test
    void evolutionDescriptionTooLong_fails() {
        String longDesc = "a".repeat(2001);
        AnamnesisRequest request = new AnamnesisRequest(
                OffsetDateTime.now(),
                longDesc,
                5
        );

        Set<ConstraintViolation<AnamnesisRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("evolutionDescription");
    }
}