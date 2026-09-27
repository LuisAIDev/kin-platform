package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.dto.validator.Cie10CodeValidator;
import com.kinplatform.kin.health.hce.dto.validator.ValidCie10Code;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class Cie10CodeValidatorTest {

    private Cie10CodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new Cie10CodeValidator();
    }

    @Test
    void nullValue_isValid() {
        boolean result = validator.isValid(null, null);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A00", "A00.0", "A00.00", "Z99.9", "J18.9", "I21.00", "K35.80"})
    void validCie10Codes_returnsTrue(String code) {
        boolean result = validator.isValid(code, null);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A0", "ZZZ", "123", "A0000", "A00.000", "a00", "A00.", "A00.0.", "", "A00.0.0"})
    void invalidCie10Codes_returnsFalse(String code) {
        boolean result = validator.isValid(code, null);
        assertThat(result).isFalse();
    }

    @Test
    void annotationExists() {
        assertThat(ValidCie10Code.class).isAnnotation();
    }
}