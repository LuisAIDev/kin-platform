package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseGenerateRequest;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseProjectResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void generateRequest_sinAsync_deberiaViolarNotNull() {
        var violations = validator.validate(new EnterpriseGenerateRequest(null, null));

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("async")));
    }

    @Test
    void generateRequest_conVersionCero_deberiaViolarPositive() {
        var violations = validator.validate(new EnterpriseGenerateRequest(false, 0));

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("requestedVersion")));
    }

    @Test
    void generateRequest_conVersionNegativa_deberiaViolarPositive() {
        var violations = validator.validate(new EnterpriseGenerateRequest(false, -3));

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("requestedVersion")));
    }

    @Test
    void generateRequest_valido_noDeberiaViolar() {
        var violations = validator.validate(new EnterpriseGenerateRequest(false, 2));

        assertTrue(violations.isEmpty());
    }

    @Test
    void generateRequest_asincrono_sinVersion_noDeberiaViolar() {
        var violations = validator.validate(new EnterpriseGenerateRequest(true, null));

        assertTrue(violations.isEmpty());
    }

    @Test
    void projectResponse_valido_noDeberiaViolar() {
        var now = OffsetDateTime.now();
        var response = new EnterpriseProjectResponse(
            UUID.randomUUID(), 1, "COMPLETED", now, now, now, null, 0, List.of());

        assertTrue(validator.validate(response).isEmpty());
    }
}
