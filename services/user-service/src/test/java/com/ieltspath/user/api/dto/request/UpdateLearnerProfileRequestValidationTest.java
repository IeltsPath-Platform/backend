package com.ieltspath.user.api.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateLearnerProfileRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static UpdateLearnerProfileRequest profile(String displayName) {
        return new UpdateLearnerProfileRequest(displayName, null, null, null, null, null);
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void displayNameAllowsAnyFullNameThatTheAccountCanHave() {
        assertThat(validator.validate(profile("n".repeat(150)))).isEmpty();
        assertThat(messages(validator.validate(profile("n".repeat(151)))))
                .containsExactly("Tên hiển thị không được vượt quá 150 ký tự");
    }

    @Test
    void displayNameIsRequired() {
        assertThat(messages(validator.validate(profile("  "))))
                .containsExactly("Tên hiển thị không được để trống");
    }
}
