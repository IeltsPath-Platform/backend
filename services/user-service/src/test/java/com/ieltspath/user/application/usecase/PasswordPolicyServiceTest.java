package com.ieltspath.user.application.usecase;

import com.ieltspath.user.config.AuthPasswordProperties;
import com.ieltspath.user.domain.exception.WeakPasswordException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyServiceTest {
    private final PasswordPolicyService policy = new PasswordPolicyService(
            password -> password.equalsIgnoreCase("password") || password.equalsIgnoreCase("matkhau123"),
            new AuthPasswordProperties(8, 12));

    @Test
    void reportsEveryApplicablePasswordReasonWithoutEchoingPassword() {
        assertThatThrownBy(() -> policy.check("password", "person@example.com", "Person Name", null))
                .isInstanceOfSatisfying(WeakPasswordException.class, error ->
                        assertThat(error.reasons()).containsExactly("TOO_COMMON"));
        assertThatThrownBy(() -> policy.check("student99", "student99@example.com", "Different Name", null))
                .isInstanceOfSatisfying(WeakPasswordException.class, error ->
                        assertThat(error.reasons()).contains("CONTAINS_ACCOUNT_INFO"));
        assertThatThrownBy(() -> policy.check("aaaaaaaa", "person@example.com", "Person Name", null))
                .isInstanceOfSatisfying(WeakPasswordException.class, error ->
                        assertThat(error.reasons()).contains("TOO_SIMPLE"));
        assertThatThrownBy(() -> policy.check("12345678", "person@example.com", "Person Name", null))
                .isInstanceOfSatisfying(WeakPasswordException.class, error ->
                        assertThat(error.reasons()).contains("TOO_SIMPLE"));
    }

    @Test
    void rejectsSevenCharactersAndAcceptsEightRandomCharacters() {
        assertThatThrownBy(() -> policy.check("Abc!234", null, null, null))
                .isInstanceOfSatisfying(WeakPasswordException.class, error ->
                        assertThat(error.reasons()).containsExactly("TOO_SHORT"));
        policy.check("R4ndom!x", "person@example.com", "Person Name", "0123456789");
    }

    @Test
    void acceptsMaximumBcryptByteLengthAndRejectsLongerInput() {
        policy.check("Aa!" + "x".repeat(69), null, null, null);
        assertThatThrownBy(() -> policy.check("ậ".repeat(25), null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mật khẩu phải có từ 8 đến 72 ký tự (chữ có dấu tính nhiều byte hơn)");
    }

    @Test
    void checksPhoneNumberAndVietnameseNameAsAccountInformation() {
        assertThatThrownBy(() -> policy.check("0123456789", "person@example.com", "Person Name", "0123456789"))
                .isInstanceOfSatisfying(WeakPasswordException.class, error ->
                        assertThat(error.reasons()).contains("CONTAINS_ACCOUNT_INFO"));
        assertThatThrownBy(() -> policy.check("nguyenvana", "person@example.com", "NguyenVanA", null))
                .isInstanceOfSatisfying(WeakPasswordException.class, error ->
                        assertThat(error.reasons()).contains("CONTAINS_ACCOUNT_INFO"));
    }
}
