package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.request.ForgotPasswordRequest;
import com.ieltspath.user.api.dto.request.GoogleLoginRequest;
import com.ieltspath.user.api.dto.request.LoginRequest;
import com.ieltspath.user.api.dto.request.LogoutRequest;
import com.ieltspath.user.api.dto.request.RefreshRequest;
import com.ieltspath.user.api.dto.request.ResendVerificationRequest;
import com.ieltspath.user.api.dto.request.ResetPasswordRequest;
import com.ieltspath.user.api.dto.request.VerifyEmailRequest;
import com.ieltspath.user.api.dto.response.AuthTokenResponse;
import com.ieltspath.user.api.dto.response.CurrentUserResponse;
import com.ieltspath.user.api.dto.response.MessageResponse;
import com.ieltspath.user.application.command.ForgotPasswordCommand;
import com.ieltspath.user.application.command.ResetPasswordCommand;
import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.application.result.GoogleSignInResult;
import com.ieltspath.user.application.usecase.ForgotPasswordUseCase;
import com.ieltspath.user.application.usecase.GoogleSignInUseCase;
import com.ieltspath.user.application.usecase.GetUserByIdUseCase;
import com.ieltspath.user.application.usecase.AuditedLoginUseCase;
import com.ieltspath.user.application.usecase.LogoutUseCase;
import com.ieltspath.user.application.usecase.RefreshTokenUseCase;
import com.ieltspath.user.application.usecase.EmailVerificationUseCase;
import com.ieltspath.user.application.usecase.ResetPasswordUseCase;
import com.ieltspath.user.api.cookie.AuthCookieService;
import com.ieltspath.user.api.request.LoginContextResolver;
import com.ieltspath.user.domain.aggregate.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuditedLoginUseCase loginUseCase;
    private final GoogleSignInUseCase googleSignInUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final EmailVerificationUseCase emailVerificationUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final AuthCookieService authCookieService;

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(new ForgotPasswordCommand(request.email()));
        return new MessageResponse("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(new ResetPasswordCommand(request.token(), request.newPassword()));
        return new MessageResponse("Đặt lại mật khẩu thành công");
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationUseCase.verify(request.token());
        return new MessageResponse("Xác minh email thành công");
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@RequestBody ResendVerificationRequest request) {
        emailVerificationUseCase.resend(request.email());
        return new MessageResponse("Nếu email chưa được xác minh, liên kết xác minh mới sẽ được gửi.");
    }

    @PostMapping("/login")
    public AuthTokenResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AuthTokenResult tokens = loginUseCase.execute(
                request.usernameOrEmail(), request.password(), LoginContextResolver.from(httpRequest));
        authCookieService.addAuthCookies(
                response,
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessTokenExpiresInSeconds(),
                tokens.refreshTokenExpiresInSeconds()
        );
        return tokenResponse(tokens, "Login successful");
    }

    @PostMapping("/google")
    public AuthTokenResponse googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        GoogleSignInResult result = googleSignInUseCase.execute(
                request.idToken(), LoginContextResolver.from(httpRequest));
        AuthTokenResult tokens = result.tokens();
        authCookieService.addAuthCookies(response, tokens.accessToken(), tokens.refreshToken(),
                tokens.accessTokenExpiresInSeconds(), tokens.refreshTokenExpiresInSeconds());
        return tokenResponse(tokens, "Login successful");
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(
            @CookieValue(name = "${app.auth.cookie.refresh-token-name:refresh_token}", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshRequest request,
            HttpServletResponse response
    ) {
        String token = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (request == null ? null : request.refreshToken());
        AuthTokenResult tokens = refreshTokenUseCase.execute(token);
        authCookieService.addAuthCookies(
                response,
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessTokenExpiresInSeconds(),
                tokens.refreshTokenExpiresInSeconds()
        );
        return tokenResponse(tokens, "Token refreshed");
    }

    @PostMapping("/logout")
    public MessageResponse logout(
            @CookieValue(name = "${app.auth.cookie.refresh-token-name:refresh_token}", required = false) String cookieRefreshToken,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletResponse response
    ) {
        String token = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (request == null ? null : request.refreshToken());
        logoutUseCase.execute(token);
        authCookieService.clearCookies(response);
        return new MessageResponse("Logout successful");
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        User user = getUserByIdUseCase.execute(currentUserProvider.requireUserId());
        return new CurrentUserResponse(
                user.getId().toString(),
                user.getId(),
                user.getEmail().value(),
                roles(user)
        );
    }

    private AuthTokenResponse tokenResponse(AuthTokenResult tokens, String message) {
        return new AuthTokenResponse(
                message,
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                tokens.accessTokenExpiresInSeconds(),
                tokens.refreshTokenExpiresInSeconds()
        );
    }

    private List<String> roles(User user) {
        return user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();
    }
}
