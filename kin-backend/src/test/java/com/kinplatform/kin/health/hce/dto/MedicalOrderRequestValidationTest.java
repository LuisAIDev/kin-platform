package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.MedicalOrder.OrderType;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Priority;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Route;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalOrderRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_medication_passes() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.MEDICATION)
                .priority(Priority.ROUTINE)
                .drugName("Ibuprofeno")
                .dose("400")
                .doseUnit("mg")
                .route(Route.ORAL)
                .frequency("Cada 8 horas")
                .durationDays(5)
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void happyPath_procedure_withCupsCode_passes() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.PROCEDURE)
                .priority(Priority.URGENT)
                .cupsCode("890201")
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullOrderType_fails() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(null)
                .priority(Priority.ROUTINE)
                .drugName("Ibuprofeno")
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("orderType");
    }

    @Test
    void nullPriority_fails() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.MEDICATION)
                .priority(null)
                .drugName("Ibuprofeno")
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("priority");
    }

    @Test
    void nullTreatmentPlanId_fails() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(null)
                .orderType(OrderType.MEDICATION)
                .priority(Priority.ROUTINE)
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("treatmentPlanId");
    }

    @Test
    void cupsCodeTooLong_fails() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.PROCEDURE)
                .priority(Priority.ROUTINE)
                .cupsCode("1".repeat(21))
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("cupsCode");
    }

    @Test
    void procedureWithoutCupsCode_fails() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.PROCEDURE)
                .priority(Priority.ROUTINE)
                .cupsCode(null)
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("cupsCode es obligatorio");
    }

    @Test
    void labExamWithoutCupsCode_fails() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.LAB_EXAM)
                .priority(Priority.ROUTINE)
                .cupsCode("")
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("cupsCode es obligatorio");
    }

    @Test
    void imagingWithoutCupsCode_fails() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.IMAGING)
                .priority(Priority.ROUTINE)
                .cupsCode("   ")
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("cupsCode es obligatorio");
    }

    @Test
    void medicationWithoutCupsCode_passes() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.MEDICATION)
                .priority(Priority.ROUTINE)
                .drugName("Ibuprofeno")
                .cupsCode(null)
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void drugNameTooLong_fails() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.MEDICATION)
                .priority(Priority.ROUTINE)
                .drugName("a".repeat(201))
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("drugName");
    }

    @Test
    void doseTooLong_fails() {
        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .treatmentPlanId(UUID.randomUUID())
                .orderType(OrderType.MEDICATION)
                .priority(Priority.ROUTINE)
                .drugName("Ibuprofeno")
                .dose("a".repeat(101))
                .build();

        Set<ConstraintViolation<CreateMedicalOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("dose");
    }
}
