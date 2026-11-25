package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final AccessTokenRevocationStore accessTokens;

    @Transactional
    public void execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(tokenHashService.hash(refreshToken))
                .ifPresent(storedToken -> {
                    storedToken.revoke(LocalDateTime.now(), "LOGOUT");
                    refreshTokenRepository.save(storedToken);
                    if (storedToken.getFamilyId() != null) accessTokens.revokeSession(storedToken.getFamilyId());
                });
    }
}
