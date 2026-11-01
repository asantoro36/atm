package com.demo.atm.infrastructure.web.request;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NotNull
@Pattern(regexp = "[0-9]{16}")
@JacksonAnnotationsInside
@JsonDeserialize(using = StrictStringDeserializer.class)
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CardNumber {

    String message() default "Invalid card number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
