package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.PasswordChangedV1;
import com.ieltspath.user.application.command.AdminResetPasswordCommand;
import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import com.ieltspath.user.application.port.AuthAuditRecorder;
import com.ieltspath.user.application.port.AuthMetrics;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.AuthAuditEvent;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.exception.WeakPasswordException;
import com.ieltspath.user.domain.repository.AccountActionTokenRepository;
import com.ieltspath.user.domain.repository.PasswordChangeAttemptRepository;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResetPasswordUseCaseTest {
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final String NEW_PASSWORD = "Moi-Matkhau-2026";

    @Mock UserRepository users;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock UserLock userLock;
    @Mock PasswordChangeAttemptRepository attempts;
    @Mock AccountActionTokenRepository actionTokens;
    @Mock UserNoticeOutbox notices;
    @Mock AccessTokenRevocationStore accessTokens;
    @Mock AuthAuditRecorder auditRecorder;
    @Mock AuthMetrics metrics;

    private final UUID adminId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private AdminResetPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AdminResetPasswordUseCase(users, refreshTokens, ENCODER, userLock, attempts, actionTokens, notices,
                new PasswordPolicyService(password -> false, new com.ieltspath.user.config.AuthPasswordProperties(8, 12)),
                accessTokens, new AuthAuditSupport(auditRecorder, () -> LoginContext.UNKNOWN), metrics);
    }

    private User.UserBuilder account() {
        return User.builder().id(userId).email(new Email("person@example.com")).fullName("Person")
                .status(UserStatus.LOCKED).passwordHash(ENCODER.encode("Cu-Matkhau-2026"));
    }

    @Test
    void replacesThePasswordSignsTheUserOutAndTellsThem() {
        User user = account().build();
        when(users.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute(new AdminResetPasswordCommand(adminId, userId, NEW_PASSWORD));

        assertThat(ENCODER.matches(NEW_PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(ENCODER.matches("Cu-Matkhau-2026", user.getPasswordHash())).isFalse();
        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(user.isEmailVerified()).isFalse();
        verify(users).save(user);
        verify(refreshTokens).revokeAllActiveForUser(eq(userId), any(), eq("PASSWORD_RESET"));
        verify(accessTokens).revokeUser(eq(userId), any());
        verify(attempts).deleteByUserId(userId);
        verify(actionTokens).invalidateUnusedForUser(eq(userId), eq(ActionTokenPurpose.PASSWORD_RESET), any());
        verify(notices).enqueue(any(), eq(userId), eq(PasswordChangedV1.EVENT_TYPE), any(PasswordChangedV1.class));

        ArgumentCaptor<AuthAuditEvent> audit = ArgumentCaptor.forClass(AuthAuditEvent.class);
        verify(auditRecorder).record(audit.capture());
        assertThat(audit.getValue().eventType()).isEqualTo("PASSWORD_RESET");
        assertThat(audit.getValue().userId()).isEqualTo(userId);
        assertThat(audit.getValue().detail()).containsEntry("method", "ADMIN").containsEntry("actorId", adminId);
        assertThat(audit.getValue().detail().toString()).doesNotContain(NEW_PASSWORD);
    }

    @Test
    void refusesTheAdministratorsOwnAccount() {
        assertThatThrownBy(() -> useCase.execute(new AdminResetPasswordCommand(adminId, adminId, NEW_PASSWORD)))
                .isInstanceOf(UserManagementRuleViolationException.class)
                .extracting("code").isEqualTo("SELF_PASSWORD_RESET");
        verifyNoInteractions(users, refreshTokens, accessTokens, notices);
    }

    @Test
    void refusesAnAccountThatSignsInWithGoogleOnly() {
        when(users.findById(userId)).thenReturn(Optional.of(account().passwordHash(null).build()));

        assertThatThrownBy(() -> useCase.execute(new AdminResetPasswordCommand(adminId, userId, NEW_PASSWORD)))
                .isInstanceOf(UserManagementRuleViolationException.class)
                .extracting("code").isEqualTo("PASSWORD_NOT_CONFIGURED");
        verify(users, never()).save(any());
        verifyNoInteractions(refreshTokens, accessTokens, notices);
    }

    @Test
    void refusesADeletedOrMissingAccount() {
        when(users.findById(userId))
                .thenReturn(Optional.of(account().status(UserStatus.DELETED).passwordHash(null).build()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new AdminResetPasswordCommand(adminId, userId, NEW_PASSWORD)))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> useCase.execute(new AdminResetPasswordCommand(adminId, userId, NEW_PASSWORD)))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(refreshTokens, accessTokens, notices);
    }

    @Test
    void refusesAWeakPassword() {
        when(users.findById(userId)).thenReturn(Optional.of(account().build()));

        assertThatThrownBy(() -> useCase.execute(new AdminResetPasswordCommand(adminId, userId, "123")))
                .isInstanceOf(WeakPasswordException.class);
        verify(users, never()).save(any());
        verifyNoInteractions(refreshTokens, accessTokens, notices);
    }
}
