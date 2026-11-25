package com.ieltspath.user.api.exception;

import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.application.exception.GoogleAccountConflictException;
import com.ieltspath.user.application.exception.GoogleEmailNotSupportedException;
import com.ieltspath.user.application.exception.GoogleSignInNotAllowedException;
import com.ieltspath.user.application.exception.GoogleSignInUnavailableException;
import com.ieltspath.user.application.exception.LoginLimiterUnavailableException;
import com.ieltspath.user.application.exception.LoginRateLimitedException;
import com.ieltspath.user.application.exception.GoogleAccountNotActiveException;
import com.ieltspath.user.application.exception.GoogleNonceRejectedException;
import com.ieltspath.user.application.exception.ReauthRequiredException;
import com.ieltspath.user.application.exception.ReauthAccountMismatchException;
import com.ieltspath.user.application.exception.PasswordAlreadySetException;
import com.ieltspath.user.application.exception.ReauthUnavailableException;
import com.ieltspath.user.domain.exception.WeakPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Duration;

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

    @Test
    void googleAccountConflictKeepsItsStatusAndCodeWithVietnameseMessage() {
        when(request.getRequestURI()).thenReturn("/auth/google");
        var response = handler.handleGoogleConflict(new GoogleAccountConflictException(), request);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("đã liên kết");
        assertThat(response.getBody().code()).isEqualTo("GOOGLE_ACCOUNT_CONFLICT");
    }

    @Test
    void googleForbiddenErrorsKeepTheirCodesAndVietnameseMessages() {
        when(request.getRequestURI()).thenReturn("/auth/google");
        var email = handler.handleGoogleForbidden(new GoogleEmailNotSupportedException(), request);
        var role = handler.handleGoogleForbidden(new GoogleSignInNotAllowedException(), request);
        assertThat(email.getStatusCode().value()).isEqualTo(403);
        assertThat(email.getBody().message()).contains("Email Google");
        assertThat(role.getBody().message()).contains("Google");
        assertThat(email.getBody().code()).isEqualTo("GOOGLE_EMAIL_NOT_SUPPORTED");
        assertThat(role.getBody().code()).isEqualTo("GOOGLE_SIGNIN_NOT_ALLOWED");
    }

    @Test
    void googleUnavailableKeepsItsCodeAndVietnameseMessage() {
        when(request.getRequestURI()).thenReturn("/auth/google");
        var response = handler.handleGoogleUnavailable(new GoogleSignInUnavailableException(), request);
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().message()).contains("Đăng nhập Google");
        assertThat(response.getBody().code()).isEqualTo("GOOGLE_SIGNIN_UNAVAILABLE");
    }

    @Test
    void loginLimiterErrorsRemainLocalizedAndPreserveRetryAfterAndCodes() {
        when(request.getRequestURI()).thenReturn("/auth/login");
        var limited = handler.handleLoginRateLimited(new LoginRateLimitedException(Duration.ofSeconds(12)), request);
        var unavailable = handler.handleLoginLimiterUnavailable(new LoginLimiterUnavailableException(null), request);
        assertThat(limited.getStatusCode().value()).isEqualTo(429);
        assertThat(limited.getHeaders().getFirst("Retry-After")).isEqualTo("12");
        assertThat(limited.getBody().message()).contains("đăng nhập");
        assertThat(limited.getBody().code()).isEqualTo("LOGIN_RATE_LIMITED");
        assertThat(unavailable.getStatusCode().value()).isEqualTo(503);
        assertThat(unavailable.getBody().message()).contains("Đăng nhập tạm thời");
        assertThat(unavailable.getBody().code()).isEqualTo("LOGIN_TEMPORARILY_UNAVAILABLE");
    }

    @Test
    void weakPasswordResponseIncludesStableCodeAndReasons() {
        when(request.getRequestURI()).thenReturn("/api/users/register");
        var response = handler.handleWeakPassword(new WeakPasswordException(java.util.List.of("TOO_COMMON", "TOO_SIMPLE")), request);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Mật khẩu chưa đủ mạnh.");
        assertThat(response.getBody().code()).isEqualTo("WEAK_PASSWORD");
        assertThat(response.getBody().details()).containsEntry("reasons", "TOO_COMMON,TOO_SIMPLE")
                .doesNotContainKey("code");
    }

    @Test
    void inactiveGoogleAccountResponseUsesVietnameseWithoutChangingAuthenticationStatus() {
        when(request.getRequestURI()).thenReturn("/auth/google");
        var response = handler.handleAuthenticationFailed(
                new GoogleAccountNotActiveException(java.util.UUID.randomUUID(),
                        com.ieltspath.user.domain.vo.UserStatus.LOCKED), request);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().message()).contains("Tài khoản không còn hoạt động");
    }

    @Test
    void invalidGoogleNonceUsesTheGenericVietnamese401AndTopLevelCode() {
        when(request.getRequestURI()).thenReturn("/auth/google");
        var response = handler.handleInvalidGoogleToken(new GoogleNonceRejectedException(), request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.");
        assertThat(response.getBody().code()).isEqualTo("GOOGLE_ID_TOKEN_INVALID");
        assertThat(response.getBody().details()).isNull();
    }

    @Test
    void sensitiveActionErrorsUseTopLevelCodesAndVietnameseMessages() {
        when(request.getRequestURI()).thenReturn("/api/users/me/oauth/google");
        var required = handler.handleReauthRequired(new ReauthRequiredException(), request);
        var mismatch = handler.handleReauthAccountMismatch(new ReauthAccountMismatchException(), request);
        var alreadySet = handler.handlePasswordAlreadySet(new PasswordAlreadySetException(), request);
        var unavailable = handler.handleReauthUnavailable(new ReauthUnavailableException(), request);

        assertThat(required.getStatusCode().value()).isEqualTo(403);
        assertThat(required.getBody().code()).isEqualTo("REAUTH_REQUIRED");
        assertThat(required.getBody().message()).isEqualTo("Vui lòng xác thực lại để thực hiện thao tác này.");
        assertThat(mismatch.getBody().code()).isEqualTo("REAUTH_ACCOUNT_MISMATCH");
        assertThat(mismatch.getBody().message()).isEqualTo("Tài khoản Google không khớp với tài khoản Google đã liên kết.");
        assertThat(alreadySet.getStatusCode().value()).isEqualTo(409);
        assertThat(alreadySet.getBody().code()).isEqualTo("PASSWORD_ALREADY_SET");
        assertThat(unavailable.getStatusCode().value()).isEqualTo(503);
        assertThat(unavailable.getBody().code()).isEqualTo("REAUTH_UNAVAILABLE");
    }
}
