package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.PasswordChangedV1;
import com.ieltspath.user.application.exception.PasswordAlreadySetException;
import com.ieltspath.user.application.exception.ReauthRequiredException;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import com.ieltspath.user.application.port.ReauthTokenStore;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.config.AuthPasswordProperties;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetInitialPasswordUseCaseTest {
    @Mock UserRepository users;
    @Mock UserLock lock;
    @Mock ReauthTokenStore reauthTokens;
    @Mock PasswordEncoder encoder;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock AccessTokenRevocationStore accessTokens;
    @Mock UserNoticeOutbox notices;
    private SetInitialPasswordUseCase useCase;
    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final User user = User.builder().id(userId).email(new Email("person@example.test"))
            .fullName("Person").status(UserStatus.ACTIVE).emailVerifiedAt(LocalDateTime.now())
            .passwordClearedAt(LocalDateTime.now()).build();

    @BeforeEach
    void setUp() {
        var policy = new PasswordPolicyService(password -> false, new AuthPasswordProperties(8, 12));
        useCase = new SetInitialPasswordUseCase(users, lock, reauthTokens, encoder, policy,
                refreshTokens, accessTokens, notices);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(reauthTokens.consume("reauth-token")).thenReturn(Optional.of(
                new ReauthTokenStore.Grant(userId, "GOOGLE", sessionId)));
        when(encoder.encode("strong-password")).thenReturn("encoded-password");
    }

    @Test
    void setsPasswordClearsTheRemovedPasswordMarkerRevokesTokensAndPublishesNotice() {
        useCase.execute(userId, sessionId, "reauth-token", "strong-password");

        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(user.getPasswordClearedAt()).isNull();
        verify(users).save(user);
        verify(refreshTokens).revokeAllActiveForUser(eq(userId), any(), eq("PASSWORD_CHANGE"));
        verify(accessTokens).revokeUser(eq(userId), any());
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(notices).enqueue(any(UUID.class), eq(userId), eq(PasswordChangedV1.EVENT_TYPE), event.capture());
        assertThat(event.getValue()).isInstanceOf(PasswordChangedV1.class);
    }

    @Test
    void requiresGoogleReauthenticationAndRejectsPasswordMethodTokens() {
        when(reauthTokens.consume("reauth-token")).thenReturn(Optional.of(
                new ReauthTokenStore.Grant(userId, "PASSWORD", sessionId)));

        assertThatThrownBy(() -> useCase.execute(userId, sessionId, "reauth-token", "strong-password"))
                .isInstanceOf(ReauthRequiredException.class);
    }

    @Test
    void rejectsTokensIssuedForAnotherUserOrSession() {
        when(reauthTokens.consume("reauth-token")).thenReturn(Optional.of(
                new ReauthTokenStore.Grant(UUID.randomUUID(), "GOOGLE", sessionId)));
        assertThatThrownBy(() -> useCase.execute(userId, sessionId, "reauth-token", "strong-password"))
                .isInstanceOf(ReauthRequiredException.class);
        when(reauthTokens.consume("reauth-token")).thenReturn(Optional.of(
                new ReauthTokenStore.Grant(userId, "GOOGLE", UUID.randomUUID())));
        assertThatThrownBy(() -> useCase.execute(userId, sessionId, "reauth-token", "strong-password"))
                .isInstanceOf(ReauthRequiredException.class);
    }

    @Test
    void rejectsExistingPasswordsAndReusesTheNewPasswordCode() {
        user.changePassword("existing-hash");

        assertThatThrownBy(() -> useCase.execute(userId, sessionId, "reauth-token", "strong-password"))
                .isInstanceOf(PasswordAlreadySetException.class)
                .hasMessage("Tài khoản đã có mật khẩu. Hãy dùng chức năng đổi mật khẩu.");
    }
}
