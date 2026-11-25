package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.PasswordChangedV1;
import com.ieltspath.user.application.exception.PasswordAlreadySetException;
import com.ieltspath.user.application.exception.ReauthRequiredException;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import com.ieltspath.user.application.port.ReauthTokenStore;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.EmailNotVerifiedException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import com.ieltspath.user.application.port.AuthMetrics;

@Service
@RequiredArgsConstructor
public class SetInitialPasswordUseCase {
    private final UserRepository users;
    private final UserLock userLock;
    private final ReauthTokenStore reauthTokens;
    private final PasswordEncoder encoder;
    private final PasswordPolicyService passwordPolicy;
    private final RefreshTokenRepository refreshTokens;
    private final AccessTokenRevocationStore accessTokens;
    private final UserNoticeOutbox notices;
    private AuthAuditSupport authAudit;
    private AuthMetrics authMetrics;

    @Autowired
    void setAuthAudit(AuthAuditSupport authAudit) { this.authAudit = authAudit; }

    @Autowired
    void setAuthMetrics(AuthMetrics authMetrics) { this.authMetrics = authMetrics; }

    @Transactional
    public void execute(UUID userId, UUID sessionId, String reauthToken, String newPassword) {
        if (sessionId == null) throw new ReauthRequiredException();
        userLock.lock(userId);
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) throw new PasswordAlreadySetException();
        if (!user.isEmailVerified()) throw new EmailNotVerifiedException();
        if (com.ieltspath.user.domain.vo.PasswordPolicy.exceedsMaxBytes(newPassword)) {
            throw new IllegalArgumentException(com.ieltspath.user.domain.vo.PasswordPolicy.MESSAGE);
        }
        passwordPolicy.check(newPassword, user.getEmail().value(), user.getFullName(),
                user.getPhoneNumber() == null ? null : user.getPhoneNumber().value());
        var grant = consume(reauthToken);
        if (grant == null || !grant.userId().equals(userId) || !grant.sessionId().equals(sessionId)
                || !"GOOGLE".equals(grant.method())) throw new ReauthRequiredException();

        user.changePassword(encoder.encode(newPassword));
        users.save(user);
        LocalDateTime now = LocalDateTime.now();
        refreshTokens.revokeAllActiveForUser(userId, now, "PASSWORD_CHANGE");
        accessTokens.revokeUser(userId, Instant.now());
        UUID eventId = UUID.randomUUID();
        notices.enqueue(eventId, userId, PasswordChangedV1.EVENT_TYPE, new PasswordChangedV1(eventId,
                PasswordChangedV1.EVENT_TYPE, Instant.now(), userId, user.getEmail().value(), user.getFullName()));
        if (authAudit != null) authAudit.record("PASSWORD_SET", userId, null, java.util.Map.of());
        if (authMetrics != null) authMetrics.record("auth.password", "event", "set");
        if (authMetrics != null) authMetrics.record("auth.session.revoked", "reason", "PASSWORD_CHANGE");
    }

    private ReauthTokenStore.Grant consume(String token) {
        try { return reauthTokens.consume(token).orElse(null); }
        catch (RuntimeException unavailable) { return null; }
    }
}
