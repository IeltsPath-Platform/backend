package com.group01.user.application.usecase;

import com.group01.user.application.result.AuthTokenResult;
import com.group01.user.domain.aggregate.RefreshToken;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.AuthenticationFailedException;
import com.group01.user.domain.repository.RefreshTokenRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenHashService tokenHashService;
    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @Test
    void rotatesUsableRefreshToken() {
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(
                refreshTokenRepository,
                userRepository,
                tokenHashService,
                authTokenIssuer
        );
        UUID userId = UUID.randomUUID();
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hashed-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        User user = activeUser(userId);
        AuthTokenResult replacementTokens = new AuthTokenResult("access-2", "refresh-2", 3600, 604800);
        when(tokenHashService.hash("refresh-1")).thenReturn("hashed-token");
        when(refreshTokenRepository.consumeActiveToken(eq("hashed-token"), any(LocalDateTime.class))).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authTokenIssuer.issue(user)).thenReturn(replacementTokens);

        AuthTokenResult result = useCase.execute("refresh-1");

        assertThat(result).isEqualTo(replacementTokens);
        verify(refreshTokenRepository, never()).save(storedToken);
        verify(authTokenIssuer).issue(user);
    }

    @Test
    void rejectsRevokedRefreshToken() {
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(
                refreshTokenRepository,
                userRepository,
                tokenHashService,
                authTokenIssuer
        );
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .tokenHash("hashed-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .revokedAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(tokenHashService.hash("refresh-1")).thenReturn("hashed-token");
        when(refreshTokenRepository.consumeActiveToken(eq("hashed-token"), any(LocalDateTime.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("refresh-1"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Refresh token không hợp lệ");
        verify(refreshTokenRepository, never()).save(storedToken);
        verifyNoInteractions(userRepository, authTokenIssuer);
    }

    @Test
    void rejectsRefreshTokenForInactiveUser() {
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(
                refreshTokenRepository,
                userRepository,
                tokenHashService,
                authTokenIssuer
        );
        UUID userId = UUID.randomUUID();
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hashed-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        User user = User.builder()
                .id(userId)
                .email(new Email("john@example.com"))
                .fullName("John Doe")
                .status(UserStatus.INACTIVE)
                .build();
        when(tokenHashService.hash("refresh-1")).thenReturn("hashed-token");
        when(refreshTokenRepository.consumeActiveToken(eq("hashed-token"), any(LocalDateTime.class))).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute("refresh-1"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Refresh token không hợp lệ");
        verify(refreshTokenRepository, never()).save(storedToken);
        verifyNoInteractions(authTokenIssuer);
    }

    private User activeUser(UUID userId) {
        return User.builder()
                .id(userId)
                .email(new Email("john@example.com"))
                .fullName("John Doe")
                .status(UserStatus.ACTIVE)
                .build();
    }
}
