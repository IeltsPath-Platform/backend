package com.ieltspath.user.api.validation;

import com.ieltspath.user.domain.vo.PhoneNumber;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A phone number typed by a user: blank (no phone) or digits with an optional leading + and common separators. */
@Documented
@Constraint(validatedBy = PhoneInput.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneInput {

    String message() default "Số điện thoại không hợp lệ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PhoneInput, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || PhoneNumber.isValidInput(value);
        }
    }
}
