package com.ieltspath.user.api.exception;

import com.ieltspath.user.domain.exception.PasswordChangeException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void currentPasswordInvalidIsBadRequestAndKeepsCode() {
        when(request.getRequestURI()).thenReturn("/api/users/me/password");

        var response = handler.handlePasswordChange(
                new PasswordChangeException("CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không chính xác"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().details()).containsEntry("code", "CURRENT_PASSWORD_INVALID");
    }

    @Test
    void rateLimitedPasswordChangeRemainsTooManyRequests() {
        when(request.getRequestURI()).thenReturn("/api/users/me/password");

        var response = handler.handlePasswordChange(
                new PasswordChangeException("PASSWORD_ATTEMPTS_LIMITED", "Thử lại sau", true), request);

        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody().details()).containsEntry("code", "PASSWORD_ATTEMPTS_LIMITED");
    }
}
