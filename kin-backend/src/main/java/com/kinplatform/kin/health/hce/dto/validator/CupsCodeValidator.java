package com.kinplatform.kin.health.hce.dto.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class CupsCodeValidator implements ConstraintValidator<ValidCupsCode, String> {

    private static final Pattern CUPS_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,6}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return CUPS_PATTERN.matcher(value).matches();
    }
}