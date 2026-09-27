package com.kinplatform.kin.health.hce.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CupsCodeValidator.class)
public @interface ValidCupsCode {
    String message() default "Código CUPS inválido (alfanumérico, 1-6 caracteres)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}