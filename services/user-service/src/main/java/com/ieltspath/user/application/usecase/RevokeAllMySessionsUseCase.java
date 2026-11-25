package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import com.ieltspath.user.application.port.AuthMetrics;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;

@Service
@RequiredArgsConstructor
public class RevokeAllMySessionsUseCase {
    private final RefreshTokenRepository tokens;
    private final AccessTokenRevocationStore accessTokens;
    private AuthAuditSupport authAudit;
    private AuthMetrics authMetrics;

    @Autowired
    void setAuthAudit(AuthAuditSupport authAudit) { this.authAudit = authAudit; }

    @Autowired
    void setAuthMetrics(AuthMetrics authMetrics) { this.authMetrics = authMetrics; }

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
        if (authAudit != null) authAudit.record("SESSIONS_REVOKED_ALL", userId, null,
                java.util.Map.of("reason", exceptCurrent ? "EXCEPT_CURRENT" : "USER_REQUEST"));
        if (authMetrics != null) authMetrics.record("auth.session.revoked", "reason", "USER_REQUEST");
    }
}
