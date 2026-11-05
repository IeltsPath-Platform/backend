package com.ieltspath.user.api.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestValidationTest {

    private static final String PASSWORD_MESSAGE = "Mật khẩu phải có từ 6 đến 72 byte (chữ có dấu tính nhiều byte hơn)";
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static RegisterRequest register(String password, String fullName, String phone) {
        return new RegisterRequest("learner@example.com", password, fullName, phone, null);
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void registerPasswordIsLimitedTo72BytesNotCharacters() {
        assertThat(validator.validate(register("a".repeat(72), "Learner", null))).isEmpty();
        assertThat(messages(validator.validate(register("a".repeat(73), "Learner", null)))).containsExactly(PASSWORD_MESSAGE);
        assertThat(validator.validate(register("ậ".repeat(24), "Learner", null))).isEmpty();
        assertThat(messages(validator.validate(register("ậ".repeat(25), "Learner", null)))).containsExactly(PASSWORD_MESSAGE);
        assertThat(messages(validator.validate(register("a".repeat(5), "Learner", null)))).containsExactly(PASSWORD_MESSAGE);
    }

    @Test
    void registerFullNameIsLimitedTo150Characters() {
        assertThat(validator.validate(register("secret123", "n".repeat(150), null))).isEmpty();
        assertThat(messages(validator.validate(register("secret123", "n".repeat(151), null))))
                .containsExactly("Họ tên không được vượt quá 150 ký tự");
    }

    @Test
    void registerPhoneAcceptsSeparatorsAndRejectsLettersAndLongValues() {
        assertThat(validator.validate(register("secret123", "Learner", "0123 456 789"))).isEmpty();
        assertThat(validator.validate(register("secret123", "Learner", "  "))).isEmpty();
        assertThat(messages(validator.validate(register("secret123", "Learner", "abc"))))
                .containsExactly("Số điện thoại không hợp lệ");
        assertThat(messages(validator.validate(register("secret123", "Learner", "1".repeat(31)))))
                .contains("Số điện thoại không được vượt quá 30 ký tự");
    }

    @Test
    void createUserSharesTheSamePasswordNameAndPhoneRules() {
        var tooLong = new CreateUserRequest("admin@example.com", "ậ".repeat(25), "n".repeat(151), "abc", null);
        assertThat(messages(validator.validate(tooLong))).containsExactlyInAnyOrder(
                PASSWORD_MESSAGE, "Họ tên không được vượt quá 150 ký tự", "Số điện thoại không hợp lệ");
        assertThat(validator.validate(new CreateUserRequest("admin@example.com", "ậ".repeat(24), "n".repeat(150),
                "+84 912 345 678", null))).isEmpty();
    }

    @Test
    void updateUserLimitsNameAndPhone() {
        assertThat(validator.validate(new UpdateUserRequest("n".repeat(150), "0123456789"))).isEmpty();
        assertThat(messages(validator.validate(new UpdateUserRequest("n".repeat(151), "abc")))).containsExactlyInAnyOrder(
                "Họ tên không được vượt quá 150 ký tự", "Số điện thoại không hợp lệ");
    }

    @Test
    void changePasswordNowHasAMaximum() {
        assertThat(validator.validate(new ChangePasswordRequest("old-password", "a".repeat(72)))).isEmpty();
        assertThat(messages(validator.validate(new ChangePasswordRequest("old-password", "a".repeat(73)))))
                .containsExactly(PASSWORD_MESSAGE);
        assertThat(messages(validator.validate(new ChangePasswordRequest("old-password", "ậ".repeat(25)))))
                .containsExactly(PASSWORD_MESSAGE);
    }

    @Test
    void resetPasswordNowHasAMaximum() {
        assertThat(validator.validate(new ResetPasswordRequest("token", "a".repeat(72)))).isEmpty();
        assertThat(messages(validator.validate(new ResetPasswordRequest("token", "a".repeat(73)))))
                .containsExactly(PASSWORD_MESSAGE);
    }
}
