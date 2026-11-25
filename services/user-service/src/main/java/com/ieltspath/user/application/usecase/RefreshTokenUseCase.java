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
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenHashService tokenHashService;
    private final AuthTokenIssuer authTokenIssuer;
    private final UserLock userLock;

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthTokenResult execute(String refreshToken, LoginContext context) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationFailedException("Refresh token không hợp lệ");
        }
        String tokenHash = tokenHashService.hash(refreshToken);
        java.util.UUID userId = refreshTokenRepository.findUserIdByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException("Refresh token không hợp lệ"));
        userLock.lock(userId);
        LocalDateTime now = LocalDateTime.now();
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException("Refresh token không hợp lệ"));
        if (current.getRevokedAt() != null) {
            if ("ROTATED".equals(current.getRevokedReason())
                    && current.getRevokedAt().plusSeconds(authTokenIssuer.reuseGraceSeconds()).isBefore(now)) {
                refreshTokenRepository.revokeFamily(current.getFamilyId(), now, "REUSE_DETECTED");
            }
            throw new AuthenticationFailedException("Refresh token không hợp lệ");
        }
        if (authTokenIssuer.sessionExpired(current.getSessionStartedAt(), now)) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now, "SESSION_REVOKED");
            throw new AuthenticationFailedException("Refresh token không hợp lệ");
        }
        RefreshToken storedToken = refreshTokenRepository.consumeActiveToken(tokenHash, now, "ROTATED",
                null, now, context.ipAddress(), context.userAgent())
                .orElseThrow(() -> new AuthenticationFailedException("Refresh token không hợp lệ"));
        if (!storedToken.isUsable(now)) {
            throw new AuthenticationFailedException("Refresh token không hợp lệ");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + storedToken.getUserId()));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("Refresh token không hợp lệ");
        }

        AuthTokenResult issued = authTokenIssuer.issue(user, storedToken.getFamilyId(),
                storedToken.getSessionStartedAt(), context);
        refreshTokenRepository.updateReplacement(tokenHash, issued.refreshTokenId());
        return issued;
    }

    public AuthTokenResult execute(String refreshToken) {
        return execute(refreshToken, LoginContext.UNKNOWN);
    }
}
