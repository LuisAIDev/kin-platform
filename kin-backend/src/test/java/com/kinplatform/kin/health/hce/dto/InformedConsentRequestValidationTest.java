package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.request.InformedConsentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InformedConsentRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void happyPath_allValid_passes() {
        InformedConsentRequest request = new InformedConsentRequest(
                "Apendicectomía laparoscópica",
                "SURGICAL",
                "v1.2",
                UUID.randomUUID()
        );

        Set<ConstraintViolation<InformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankProcedureName_fails() {
        InformedConsentRequest request = new InformedConsentRequest(
                "",
                "SURGICAL",
                "v1.2",
                UUID.randomUUID()
        );

        Set<ConstraintViolation<InformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("procedureName");
    }

    @Test
    void procedureNameTooLong_fails() {
        String longName = "a".repeat(201);
        InformedConsentRequest request = new InformedConsentRequest(
                longName,
                "SURGICAL",
                "v1.2",
                UUID.randomUUID()
        );

        Set<ConstraintViolation<InformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("procedureName");
    }

    @Test
    void nullConsentType_fails() {
        InformedConsentRequest request = new InformedConsentRequest(
                "Apendicectomía",
                null,
                "v1.2",
                UUID.randomUUID()
        );

        Set<ConstraintViolation<InformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("consentType");
    }

    @Test
    void invalidConsentType_fails() {
        InformedConsentRequest request = new InformedConsentRequest(
                "Apendicectomía",
                "INVALID_TYPE",
                "v1.2",
                UUID.randomUUID()
        );

        Set<ConstraintViolation<InformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("consentType");
    }

    @Test
    void blankDocumentVersion_fails() {
        InformedConsentRequest request = new InformedConsentRequest(
                "Apendicectomía",
                "SURGICAL",
                "",
                UUID.randomUUID()
        );

        Set<ConstraintViolation<InformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("documentVersion");
    }

    @Test
    void nullPhysicianId_fails() {
        InformedConsentRequest request = new InformedConsentRequest(
                "Apendicectomía",
                "SURGICAL",
                "v1.2",
                null
        );

        Set<ConstraintViolation<InformedConsentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("physicianId");
    }
}