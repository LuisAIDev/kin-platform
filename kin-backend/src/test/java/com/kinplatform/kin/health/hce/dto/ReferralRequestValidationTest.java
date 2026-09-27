package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.Referral.Priority;
import com.kinplatform.kin.health.hce.entity.Referral.ReferralType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReferralRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        CreateReferralRequest request = CreateReferralRequest.builder()
                .patientId(UUID.randomUUID())
                .referringService("Medicina General")
                .referredToService("Neurología")
                .referredToInstitution("Hospital Universitario")
                .referralType(ReferralType.INTERCONSULTATION)
                .priority(Priority.ROUTINE)
                .reason("Paciente requiere evaluación por especialista en neurología")
                .build();

        Set<ConstraintViolation<CreateReferralRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullReferralType_fails() {
        CreateReferralRequest request = CreateReferralRequest.builder()
                .patientId(UUID.randomUUID())
                .referringService("Medicina General")
                .referredToService("Neurología")
                .referralType(null)
                .priority(Priority.ROUTINE)
                .reason("Motivo de referencia")
                .build();

        Set<ConstraintViolation<CreateReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("referralType");
    }

    @Test
    void nullPriority_fails() {
        CreateReferralRequest request = CreateReferralRequest.builder()
                .patientId(UUID.randomUUID())
                .referringService("Medicina General")
                .referredToService("Neurología")
                .referralType(ReferralType.INTERCONSULTATION)
                .priority(null)
                .reason("Motivo de referencia")
                .build();

        Set<ConstraintViolation<CreateReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("priority");
    }

    @Test
    void blankReason_fails() {
        CreateReferralRequest request = CreateReferralRequest.builder()
                .patientId(UUID.randomUUID())
                .referringService("Medicina General")
                .referredToService("Neurología")
                .referralType(ReferralType.INTERCONSULTATION)
                .priority(Priority.ROUTINE)
                .reason("")
                .build();

        Set<ConstraintViolation<CreateReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("reason");
    }

    @Test
    void reasonTooLong_fails() {
        CreateReferralRequest request = CreateReferralRequest.builder()
                .patientId(UUID.randomUUID())
                .referringService("Medicina General")
                .referredToService("Neurología")
                .referralType(ReferralType.INTERCONSULTATION)
                .priority(Priority.ROUTINE)
                .reason("a".repeat(2001))
                .build();

        Set<ConstraintViolation<CreateReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("reason");
    }

    @Test
    void blankReferredToService_fails() {
        CreateReferralRequest request = CreateReferralRequest.builder()
                .patientId(UUID.randomUUID())
                .referringService("Medicina General")
                .referredToService("")
                .referralType(ReferralType.INTERCONSULTATION)
                .priority(Priority.ROUTINE)
                .reason("Motivo de referencia")
                .build();

        Set<ConstraintViolation<CreateReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("referredToService");
    }

    @Test
    void referredToServiceTooLong_fails() {
        CreateReferralRequest request = CreateReferralRequest.builder()
                .patientId(UUID.randomUUID())
                .referringService("Medicina General")
                .referredToService("a".repeat(101))
                .referralType(ReferralType.INTERCONSULTATION)
                .priority(Priority.ROUTINE)
                .reason("Motivo de referencia")
                .build();

        Set<ConstraintViolation<CreateReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("referredToService");
    }

    @Test
    void nullPatientId_fails() {
        CreateReferralRequest request = CreateReferralRequest.builder()
                .patientId(null)
                .referringService("Medicina General")
                .referredToService("Neurología")
                .referralType(ReferralType.INTERCONSULTATION)
                .priority(Priority.ROUTINE)
                .reason("Motivo de referencia")
                .build();

        Set<ConstraintViolation<CreateReferralRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("patientId");
    }
}
