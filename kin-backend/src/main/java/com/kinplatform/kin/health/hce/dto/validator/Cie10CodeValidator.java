package com.kinplatform.kin.health.hce.dto.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class Cie10CodeValidator implements ConstraintValidator<ValidCie10Code, String> {

    private static final Pattern CIE10_PATTERN = Pattern.compile("^[A-Z]\\d{2}(\\.\\d{1,2})?$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return CIE10_PATTERN.matcher(value).matches();
    }
}