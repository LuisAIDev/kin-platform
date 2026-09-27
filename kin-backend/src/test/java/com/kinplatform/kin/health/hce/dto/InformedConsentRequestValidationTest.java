package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.InformedConsent.ConsentType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InformedConsentRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                .patientId(UUID.randomUUID())
                .procedureName("Apendicectomía laparoscópica")
                .consentType(ConsentType.SURGICAL)
                .documentVersion("v1.2")
                .build();

        Set<ConstraintViolation<CreateInformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankProcedureName_fails() {
        CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                .patientId(UUID.randomUUID())
                .procedureName("")
                .consentType(ConsentType.SURGICAL)
                .documentVersion("v1.2")
                .build();

        Set<ConstraintViolation<CreateInformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("procedureName");
    }

    @Test
    void procedureNameTooLong_fails() {
        CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                .patientId(UUID.randomUUID())
                .procedureName("a".repeat(201))
                .consentType(ConsentType.SURGICAL)
                .documentVersion("v1.2")
                .build();

        Set<ConstraintViolation<CreateInformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("procedureName");
    }

    @Test
    void nullConsentType_fails() {
        CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                .patientId(UUID.randomUUID())
                .procedureName("Apendicectomía")
                .consentType(null)
                .documentVersion("v1.2")
                .build();

        Set<ConstraintViolation<CreateInformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("consentType");
    }

    @Test
    void blankDocumentVersion_fails() {
        CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                .patientId(UUID.randomUUID())
                .procedureName("Apendicectomía")
                .consentType(ConsentType.SURGICAL)
                .documentVersion("")
                .build();

        Set<ConstraintViolation<CreateInformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("documentVersion");
    }

    @Test
    void nullPatientId_fails() {
        CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                .patientId(null)
                .procedureName("Apendicectomía")
                .consentType(ConsentType.SURGICAL)
                .documentVersion("v1.2")
                .build();

        Set<ConstraintViolation<CreateInformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("patientId");
    }
}
