package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;

@Service
@RequiredArgsConstructor
public class RevokeAllMySessionsUseCase {
    private final RefreshTokenRepository tokens;
    private final AccessTokenRevocationStore accessTokens;

    @Transactional
    public void execute(UUID userId, UUID currentSessionId, boolean exceptCurrent) {
        LocalDateTime now = LocalDateTime.now();
        var revokedSessions = tokens.findActiveSessionIds(userId, now,
                exceptCurrent ? currentSessionId : null);
        if (exceptCurrent && currentSessionId != null) {
            tokens.revokeAllActiveExceptSession(userId, currentSessionId, now, "SESSION_REVOKED");
        } else {
            tokens.revokeAllActiveForUser(userId, now, "SESSION_REVOKED");
        }
        revokedSessions.forEach(accessTokens::revokeSession);
    }
}
