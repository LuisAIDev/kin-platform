package com.kinplatform.kin.health.hce.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Cie10CodeValidator.class)
public @interface ValidCie10Code {
    String message() default "Formato CIE-10 inválido (ej: A00, A00.0, A00.00)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}