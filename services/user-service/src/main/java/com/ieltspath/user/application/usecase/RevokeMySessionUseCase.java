package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.exception.SessionNotFoundException;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import com.ieltspath.user.application.port.AuthMetrics;

@Service
@RequiredArgsConstructor
public class RevokeMySessionUseCase {
    private final RefreshTokenRepository tokens;
    private final AccessTokenRevocationStore accessTokens;
    private AuthAuditSupport authAudit;
    private AuthMetrics authMetrics;

    @Autowired
    void setAuthAudit(AuthAuditSupport authAudit) { this.authAudit = authAudit; }

    @Autowired
    void setAuthMetrics(AuthMetrics authMetrics) { this.authMetrics = authMetrics; }

    @Transactional
    public void execute(UUID userId, UUID sessionId) {
        if (tokens.revokeSession(userId, sessionId, LocalDateTime.now(), "SESSION_REVOKED") == 0) {
            throw new SessionNotFoundException();
        }
        accessTokens.revokeSession(sessionId);
        if (authAudit != null) authAudit.record("SESSION_REVOKED", userId, null,
                java.util.Map.of("sessionId", sessionId, "reason", "USER_REQUEST"));
        if (authMetrics != null) authMetrics.record("auth.session.revoked", "reason", "USER_REQUEST");
    }
}
