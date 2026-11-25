package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import com.ieltspath.user.domain.aggregate.RefreshToken;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {
    @Mock RefreshTokenRepository tokens;
    @Mock UserRepository users;
    @Mock TokenHashService hashes;
    @Mock AuthTokenIssuer issuer;
    @Mock UserLock userLock;
    @Mock AccessTokenRevocationStore accessTokens;

    @Test
    void rotatesWithinTheSameSessionAndRecordsRequestContext() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        UUID replacementId = UUID.randomUUID();
        LocalDateTime started = LocalDateTime.now().minusHours(1);
        RefreshToken token = token(userId, familyId, started, null, null);
        User user = activeUser(userId);
        LoginContext context = new LoginContext("203.0.113.45", "Browser");
        AuthTokenResult result = new AuthTokenResult("access", "next-refresh", 3600, 604800, replacementId);
        when(hashes.hash("raw")).thenReturn("hash");
        when(tokens.findUserIdByTokenHash("hash")).thenReturn(Optional.of(token.getUserId()));
        when(tokens.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));
        when(tokens.consumeActiveToken(eq("hash"), any(), eq("ROTATED"), isNull(), any(),
                eq("203.0.113.45"), eq("Browser"))).thenReturn(Optional.of(token));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(issuer.issue(user, familyId, started, context)).thenReturn(result);

        AuthTokenResult actual = useCase().execute("raw", context);

        assertThat(actual).isEqualTo(result);
        verify(tokens).updateReplacement("hash", replacementId);
    }

    @Test
    void reuseAfterGraceRevokesTheWholeSessionFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken token = token(UUID.randomUUID(), familyId, LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusSeconds(20), "ROTATED");
        when(hashes.hash("raw")).thenReturn("hash");
        when(tokens.findUserIdByTokenHash("hash")).thenReturn(Optional.of(token.getUserId()));
        when(tokens.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));
        when(issuer.reuseGraceSeconds()).thenReturn(10L);

        assertThatThrownBy(() -> useCase().execute("raw", LoginContext.UNKNOWN))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Refresh token không hợp lệ");
        verify(tokens).revokeFamily(eq(familyId), any(), eq("REUSE_DETECTED"));
        verify(accessTokens).revokeSession(familyId);
        verifyNoInteractions(users);
    }

    @Test
    void reuseWithinGraceDoesNotRevokeTheFamily() {
        RefreshToken token = token(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusSeconds(2), "ROTATED");
        when(hashes.hash("raw")).thenReturn("hash");
        when(tokens.findUserIdByTokenHash("hash")).thenReturn(Optional.of(token.getUserId()));
        when(tokens.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));
        when(issuer.reuseGraceSeconds()).thenReturn(10L);

        assertThatThrownBy(() -> useCase().execute("raw", LoginContext.UNKNOWN))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(tokens, never()).revokeFamily(any(), any(), anyString());
    }

    @Test
    void logoutRevokedTokenDoesNotTriggerReuseRevocation() {
        RefreshToken token = token(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusMinutes(1), "LOGOUT");
        when(hashes.hash("raw")).thenReturn("hash");
        when(tokens.findUserIdByTokenHash("hash")).thenReturn(Optional.of(token.getUserId()));
        when(tokens.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> useCase().execute("raw", LoginContext.UNKNOWN))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(tokens, never()).revokeFamily(any(), any(), anyString());
    }

    @Test
    void absoluteSessionExpiryRevokesFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken token = token(UUID.randomUUID(), familyId, LocalDateTime.now().minusDays(31), null, null);
        when(hashes.hash("raw")).thenReturn("hash");
        when(tokens.findUserIdByTokenHash("hash")).thenReturn(Optional.of(token.getUserId()));
        when(tokens.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));
        when(issuer.sessionExpired(eq(token.getSessionStartedAt()), any())).thenReturn(true);

        assertThatThrownBy(() -> useCase().execute("raw", LoginContext.UNKNOWN))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(tokens).revokeFamily(eq(familyId), any(), eq("SESSION_REVOKED"));
    }

    @Test
    void inactiveUserCannotRefresh() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = token(userId, UUID.randomUUID(), LocalDateTime.now().minusHours(1), null, null);
        User user = User.builder().id(userId).email(new Email("inactive@example.com"))
                .fullName("Inactive User").status(UserStatus.INACTIVE).build();
        when(hashes.hash("raw")).thenReturn("hash");
        when(tokens.findUserIdByTokenHash("hash")).thenReturn(Optional.of(token.getUserId()));
        when(tokens.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));
        when(tokens.consumeActiveToken(eq("hash"), any(), eq("ROTATED"), isNull(), any(), isNull(), isNull()))
                .thenReturn(Optional.of(token));
        when(users.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase().execute("raw", LoginContext.UNKNOWN))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(issuer, never()).issue(any(User.class), any(UUID.class), any(LocalDateTime.class), any(LoginContext.class));
    }

    private RefreshTokenUseCase useCase() {
        return new RefreshTokenUseCase(tokens, users, hashes, issuer, userLock, accessTokens);
    }

    private RefreshToken token(UUID userId, UUID familyId, LocalDateTime started,
                               LocalDateTime revokedAt, String reason) {
        return RefreshToken.builder().id(UUID.randomUUID()).userId(userId).familyId(familyId)
                .sessionStartedAt(started).tokenHash("hash").expiresAt(LocalDateTime.now().plusDays(5))
                .revokedAt(revokedAt).revokedReason(reason).build();
    }

    private User activeUser(UUID id) {
        return User.builder().id(id).email(new Email("active@example.com")).fullName("Active User")
                .status(UserStatus.ACTIVE).build();
    }
}
