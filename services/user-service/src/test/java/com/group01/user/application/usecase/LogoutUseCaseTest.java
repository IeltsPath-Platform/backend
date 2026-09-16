package com.group01.user.application.usecase;

import com.group01.user.domain.aggregate.RefreshToken;
import com.group01.user.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private TokenHashService tokenHashService;

    @Test
    void revokesRefreshTokenWhenItExists() {
        LogoutUseCase useCase = new LogoutUseCase(refreshTokenRepository, tokenHashService);
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .tokenHash("hashed-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(tokenHashService.hash("refresh-1")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(storedToken));

        useCase.execute("refresh-1");

        assertThat(storedToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void ignoresBlankRefreshToken() {
        LogoutUseCase useCase = new LogoutUseCase(refreshTokenRepository, tokenHashService);

        useCase.execute(" ");

        verifyNoInteractions(refreshTokenRepository, tokenHashService);
    }
}
