package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.domain.aggregate.RefreshToken;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import com.ieltspath.user.application.port.AuthMetrics;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenHashService tokenHashService;
    private final AuthTokenIssuer authTokenIssuer;
    private final UserLock userLock;
    private final AccessTokenRevocationStore accessTokens;
    private AuthMetrics authMetrics;
    private AuthAuditSupport authAudit;

    @Autowired
    void setAuthMetrics(AuthMetrics authMetrics) { this.authMetrics = authMetrics; }

    @Autowired
    void setAuthAudit(AuthAuditSupport authAudit) { this.authAudit = authAudit; }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthTokenResult execute(String refreshToken, LoginContext context) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidToken();
        }
        String tokenHash = tokenHashService.hash(refreshToken);
        java.util.UUID userId = refreshTokenRepository.findUserIdByTokenHash(tokenHash)
                .orElseThrow(this::invalidToken);
        userLock.lock(userId);
        LocalDateTime now = LocalDateTime.now();
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::invalidToken);
        if (current.getRevokedAt() != null) {
            if ("ROTATED".equals(current.getRevokedReason())
                    && current.getRevokedAt().plusSeconds(authTokenIssuer.reuseGraceSeconds()).isBefore(now)) {
                refreshTokenRepository.revokeFamily(current.getFamilyId(), now, "REUSE_DETECTED");
                accessTokens.revokeSession(current.getFamilyId());
                if (authAudit != null) authAudit.record("REFRESH_REUSE_DETECTED", userId, context,
                        java.util.Map.of("sessionId", current.getFamilyId(), "reason", "REUSE_DETECTED"));
                if (authMetrics != null) authMetrics.record("auth.refresh", "result", "reuse");
            }
            throw invalidToken();
        }
        if (authTokenIssuer.sessionExpired(current.getSessionStartedAt(), now)) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now, "SESSION_REVOKED");
            accessTokens.revokeSession(current.getFamilyId());
            if (authMetrics != null) authMetrics.record("auth.refresh", "result", "expired_session");
            if (authMetrics != null) authMetrics.record("auth.session.revoked", "reason", "SESSION_REVOKED");
            throw invalidToken();
        }
        RefreshToken storedToken = refreshTokenRepository.consumeActiveToken(tokenHash, now, "ROTATED",
                null, now, context.ipAddress(), context.userAgent())
                .orElseThrow(this::invalidToken);
        if (!storedToken.isUsable(now)) {
            throw invalidToken();
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + storedToken.getUserId()));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw invalidToken();
        }

        AuthTokenResult issued = authTokenIssuer.issue(user, storedToken.getFamilyId(),
                storedToken.getSessionStartedAt(), context);
        refreshTokenRepository.updateReplacement(tokenHash, issued.refreshTokenId());
        if (authMetrics != null) authMetrics.record("auth.refresh", "result", "success");
        return issued;
    }

    public AuthTokenResult execute(String refreshToken) {
        return execute(refreshToken, LoginContext.UNKNOWN);
    }

    private AuthenticationFailedException invalidToken() {
        if (authMetrics != null) authMetrics.record("auth.refresh", "result", "invalid");
        return new AuthenticationFailedException("Refresh token không hợp lệ");
    }
}
