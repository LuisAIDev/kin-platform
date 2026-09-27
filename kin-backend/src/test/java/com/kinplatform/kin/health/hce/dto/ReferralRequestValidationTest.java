package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.ReferralRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReferralRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        ReferralRequest request = new ReferralRequest(
                "INTERCONSULTATION",
                "ROUTINE",
                "Paciente requiere evaluación por especialista en neurología",
                "Neurología",
                "Hospital Universitario"
        );

        Set<ConstraintViolation<ReferralRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullReferralType_fails() {
        ReferralRequest request = new ReferralRequest(
                null,
                "ROUTINE",
                "Motivo de referencia",
                "Neurología",
                null
        );

        Set<ConstraintViolation<ReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("referralType");
    }

    @Test
    void invalidReferralType_fails() {
        ReferralRequest request = new ReferralRequest(
                "INVALID_TYPE",
                "ROUTINE",
                "Motivo de referencia",
                "Neurología",
                null
        );

        Set<ConstraintViolation<ReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("referralType");
    }

    @Test
    void nullPriority_fails() {
        ReferralRequest request = new ReferralRequest(
                "INTERCONSULTATION",
                null,
                "Motivo de referencia",
                "Neurología",
                null
        );

        Set<ConstraintViolation<ReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("priority");
    }

    @Test
    void blankReason_fails() {
        ReferralRequest request = new ReferralRequest(
                "INTERCONSULTATION",
                "ROUTINE",
                "",
                "Neurología",
                null
        );

        Set<ConstraintViolation<ReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("reason");
    }

    @Test
    void reasonTooLong_fails() {
        String longReason = "a".repeat(2001);
        ReferralRequest request = new ReferralRequest(
                "INTERCONSULTATION",
                "ROUTINE",
                longReason,
                "Neurología",
                null
        );

        Set<ConstraintViolation<ReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("reason");
    }

    @Test
    void blankReferredToService_fails() {
        ReferralRequest request = new ReferralRequest(
                "INTERCONSULTATION",
                "ROUTINE",
                "Motivo de referencia",
                "",
                null
        );

        Set<ConstraintViolation<ReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("referredToService");
    }

    @Test
    void referredToServiceTooLong_fails() {
        String longService = "a".repeat(101);
        ReferralRequest request = new ReferralRequest(
                "INTERCONSULTATION",
                "ROUTINE",
                "Motivo de referencia",
                longService,
                null
        );

        Set<ConstraintViolation<ReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("referredToService");
    }
}