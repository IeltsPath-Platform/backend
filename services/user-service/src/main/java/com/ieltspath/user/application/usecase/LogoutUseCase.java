package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;

    @Transactional
    public void execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(tokenHashService.hash(refreshToken))
                .ifPresent(storedToken -> {
                    storedToken.revoke(LocalDateTime.now());
                    refreshTokenRepository.save(storedToken);
                });
    }
}
