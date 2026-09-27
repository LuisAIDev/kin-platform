package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.PhysicalExamRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PhysicalExamRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                120,
                80,
                72,
                16,
                36.5,
                98,
                70.0,
                175.0,
                15,
                3
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void bpSystolicTooLow_fails() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                40,
                80,
                72,
                16,
                36.5,
                98,
                70.0,
                175.0,
                15,
                3
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("bpSystolic");
    }

    @Test
    void bpSystolicTooHigh_fails() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                350,
                80,
                72,
                16,
                36.5,
                98,
                70.0,
                175.0,
                15,
                3
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("bpSystolic");
    }

    @Test
    void bpDiastolicTooLow_fails() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                120,
                20,
                72,
                16,
                36.5,
                98,
                70.0,
                175.0,
                15,
                3
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("bpDiastolic");
    }

    @Test
    void heartRateTooLow_fails() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                120,
                80,
                20,
                16,
                36.5,
                98,
                70.0,
                175.0,
                15,
                3
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("heartRate");
    }

    @Test
    void temperatureTooLow_fails() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                120,
                80,
                72,
                16,
                25.0,
                98,
                70.0,
                175.0,
                15,
                3
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("temperature");
    }

    @Test
    void temperatureTooHigh_fails() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                120,
                80,
                72,
                16,
                50.0,
                98,
                70.0,
                175.0,
                15,
                3
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("temperature");
    }

    @Test
    void weightTooLow_fails() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                120,
                80,
                72,
                16,
                36.5,
                98,
                0.05,
                175.0,
                15,
                3
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("weightKg");
    }

    @Test
    void glasgowScoreTooLow_fails() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                120,
                80,
                72,
                16,
                36.5,
                98,
                70.0,
                175.0,
                2,
                3
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("glasgowScore");
    }

    @Test
    void painScaleTooHigh_fails() {
        PhysicalExamRequest request = new PhysicalExamRequest(
                120,
                80,
                72,
                16,
                36.5,
                98,
                70.0,
                175.0,
                15,
                11
        );

        Set<ConstraintViolation<PhysicalExamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("painScale");
    }
}