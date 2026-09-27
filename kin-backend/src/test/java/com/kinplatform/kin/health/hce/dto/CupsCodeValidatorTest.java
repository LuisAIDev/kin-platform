package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.validator.CupsCodeValidator;
import com.kinplatform.kin.health.hce.dto.validator.ValidCupsCode;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CupsCodeValidatorTest {

    private CupsCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CupsCodeValidator();
    }

    @Test
    void nullValue_isValid() {
        boolean result = validator.isValid(null, null);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"890201", "5DSB01", "T2387G", "A1", "ABC123", "123456", "a1b2c3", "X", "999999"})
    void validCupsCodes_returnsTrue(String code) {
        boolean result = validator.isValid(code, null);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TOOLONG", "ABC-123", "A B", "A.B", "A/B", "A#1", "", "1234567"})
    void invalidCupsCodes_returnsFalse(String code) {
        boolean result = validator.isValid(code, null);
        assertThat(result).isFalse();
    }

    @Test
    void annotationExists() {
        assertThat(ValidCupsCode.class).isAnnotation();
    }
}