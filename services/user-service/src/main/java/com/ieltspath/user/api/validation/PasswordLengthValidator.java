package com.ieltspath.user.api.validation;

import com.ieltspath.user.domain.vo.PasswordPolicy;
import com.ieltspath.user.config.AuthPasswordProperties;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordLengthValidator implements ConstraintValidator<PasswordLength, String> {
    private int minLength = 8;

    @Autowired(required = false)
    void configure(AuthPasswordProperties properties) {
        this.minLength = properties.minLength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || PasswordPolicy.isWithinLimits(value, minLength)) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(PasswordPolicy.message(minLength))
                .addConstraintViolation();
        return false;
    }
}
