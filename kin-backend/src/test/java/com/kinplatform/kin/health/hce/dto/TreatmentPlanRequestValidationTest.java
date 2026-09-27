package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.TreatmentPlanRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TreatmentPlanRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "OUTPATIENT_TREATMENT",
                List.of("Controlar dolor", "Seguimiento en 7 días"),
                "GOOD"
        );

        Set<ConstraintViolation<TreatmentPlanRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullConduct_fails() {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                null,
                List.of("Controlar dolor"),
                "GOOD"
        );

        Set<ConstraintViolation<TreatmentPlanRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("conduct");
    }

    @Test
    void invalidConduct_fails() {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "INVALID_CONDUCT",
                List.of("Controlar dolor"),
                "GOOD"
        );

        Set<ConstraintViolation<TreatmentPlanRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("conduct");
    }

    @Test
    void therapeuticGoalsTooMany_fails() {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "OUTPATIENT_TREATMENT",
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"),
                "GOOD"
        );

        Set<ConstraintViolation<TreatmentPlanRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("therapeuticGoals");
    }

    @Test
    void invalidPrognosis_fails() {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "OUTPATIENT_TREATMENT",
                List.of("Controlar dolor"),
                "INVALID_PROGNOSIS"
        );

        Set<ConstraintViolation<TreatmentPlanRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("prognosis");
    }
}