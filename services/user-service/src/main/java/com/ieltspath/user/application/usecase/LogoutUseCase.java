package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final AccessTokenRevocationStore accessTokens;
    private AuthAuditSupport authAudit;

    @Autowired
    void setAuthAudit(AuthAuditSupport authAudit) { this.authAudit = authAudit; }

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
                    if (authAudit != null) authAudit.record("SESSION_REVOKED", storedToken.getUserId(), null,
                            storedToken.getFamilyId() == null ? java.util.Map.of("reason", "LOGOUT")
                                    : java.util.Map.of("sessionId", storedToken.getFamilyId(), "reason", "LOGOUT"));
                });
    }
}
