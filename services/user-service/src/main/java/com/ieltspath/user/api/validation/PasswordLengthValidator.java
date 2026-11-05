package com.ieltspath.user.api.validation;

import com.ieltspath.user.domain.vo.PasswordPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordLengthValidator implements ConstraintValidator<PasswordLength, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || PasswordPolicy.isWithinLimits(value);
    }
}
