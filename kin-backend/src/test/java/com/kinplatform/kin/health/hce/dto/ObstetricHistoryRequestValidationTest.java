package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.ObstetricHistoryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ObstetricHistoryRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        ObstetricHistoryRequest request = new ObstetricHistoryRequest(
                3,
                2,
                1,
                0,
                0,
                "PARTIAL"
        );

        Set<ConstraintViolation<ObstetricHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullGravida_passes() {
        ObstetricHistoryRequest request = new ObstetricHistoryRequest(
                null,
                2,
                1,
                0,
                0,
                "PARTIAL"
        );

        Set<ConstraintViolation<ObstetricHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void gravidaLessThanSum_fails() {
        ObstetricHistoryRequest request = new ObstetricHistoryRequest(
                2,
                2,
                1,
                0,
                0,
                "PARTIAL"
        );

        Set<ConstraintViolation<ObstetricHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("gravida debe ser >=");
    }

    @Test
    void negativeGravida_fails() {
        ObstetricHistoryRequest request = new ObstetricHistoryRequest(
                -1,
                2,
                1,
                0,
                0,
                "PARTIAL"
        );

        Set<ConstraintViolation<ObstetricHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(2);
        assertThat(violations).extracting("propertyPath").extracting(Object::toString).contains("gravida", "gravidaConsistent");
    }

    @Test
    void negativePara_fails() {
        ObstetricHistoryRequest request = new ObstetricHistoryRequest(
                3,
                -1,
                1,
                0,
                0,
                "PARTIAL"
        );

        Set<ConstraintViolation<ObstetricHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("para");
    }

    @Test
    void invalidBreastfeedingStatus_fails() {
        ObstetricHistoryRequest request = new ObstetricHistoryRequest(
                3,
                2,
                1,
                0,
                0,
                "INVALID_STATUS"
        );

        Set<ConstraintViolation<ObstetricHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("breastfeedingStatus");
    }
}