package com.group01.user.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.user.api.dto.request.ForgotPasswordRequest;
import com.group01.user.api.dto.request.LoginRequest;
import com.group01.user.api.dto.request.LogoutRequest;
import com.group01.user.api.dto.request.RefreshRequest;
import com.group01.user.api.dto.request.ResetPasswordRequest;
import com.group01.user.api.dto.response.AuthTokenResponse;
import com.group01.user.api.dto.response.CurrentUserResponse;
import com.group01.user.api.dto.response.MessageResponse;
import com.group01.user.application.command.ForgotPasswordCommand;
import com.group01.user.application.command.ResetPasswordCommand;
import com.group01.user.application.result.AuthTokenResult;
import com.group01.user.application.usecase.ForgotPasswordUseCase;
import com.group01.user.application.usecase.GetUserByIdUseCase;
import com.group01.user.application.usecase.LoginUseCase;
import com.group01.user.application.usecase.LogoutUseCase;
import com.group01.user.application.usecase.RefreshTokenUseCase;
import com.group01.user.application.usecase.ResetPasswordUseCase;
import com.group01.user.domain.aggregate.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(new ForgotPasswordCommand(request.email()));
        return new MessageResponse("Mã xác nhận đặt lại mật khẩu đã được xử lý");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(new ResetPasswordCommand(request.token(), request.newPassword()));
        return new MessageResponse("Đặt lại mật khẩu thành công");
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(loginUseCase.execute(request.usernameOrEmail(), request.password()), "Login successful");
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(@RequestBody(required = false) RefreshRequest request) {
        return tokenResponse(refreshTokenUseCase.execute(request == null ? null : request.refreshToken()), "Token refreshed");
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestBody(required = false) LogoutRequest request) {
        logoutUseCase.execute(request == null ? null : request.refreshToken());
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
