package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.PasswordChangedV1;
import com.ieltspath.user.application.command.AdminResetPasswordCommand;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import com.ieltspath.user.application.port.AuthMetrics;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.AccountActionTokenRepository;
import com.ieltspath.user.domain.repository.PasswordChangeAttemptRepository;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.domain.vo.PasswordPolicy;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/**
 * An administrator sets a new password for a user who cannot sign in. The administrator could then sign in as that user,
 * so the notice e-mail to the user and the audit record naming the administrator must stay. The account status and the
 * e-mail verification state are left as they are.
 */
@Service
@RequiredArgsConstructor
public class AdminResetPasswordUseCase {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final UserLock userLock;
    private final PasswordChangeAttemptRepository attempts;
    private final AccountActionTokenRepository actionTokens;
    private final UserNoticeOutbox notices;
    private final PasswordPolicyService passwordPolicy;
    private final AccessTokenRevocationStore accessTokens;
    private final AuthAuditSupport authAudit;
    private final AuthMetrics authMetrics;

    @Transactional
    public void execute(AdminResetPasswordCommand command) {
        if (PasswordPolicy.exceedsMaxBytes(command.newPassword())) {
            throw new IllegalArgumentException(PasswordPolicy.MESSAGE);
        }
        UUID userId = command.userId();
        // Their own password goes through Change Password, which asks for the current one.
        if (userId.equals(command.adminId())) {
            throw new UserManagementRuleViolationException("SELF_PASSWORD_RESET",
                    "Không đặt lại mật khẩu của chính mình ở đây. Hãy dùng chức năng Đổi mật khẩu.");
        }
        userLock.lock(userId);
        User user = users.findById(userId)
                .filter(found -> found.getStatus() != UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new UserManagementRuleViolationException("PASSWORD_NOT_CONFIGURED",
                    "Tài khoản này chưa có mật khẩu. Người dùng đăng nhập bằng Google.");
        }
        passwordPolicy.check(command.newPassword(), user.getEmail().value(), user.getFullName(),
                user.getPhoneNumber() == null ? null : user.getPhoneNumber().value());

        user.changePassword(encoder.encode(command.newPassword()));
        users.save(user);
        refreshTokens.revokeAllActiveForUser(userId, LocalDateTime.now(), "PASSWORD_RESET");
        accessTokens.revokeUser(userId, Instant.now());
        attempts.deleteByUserId(userId);
        // A reset link sent earlier must not replace the password the administrator just set. Token times are UTC.
        actionTokens.invalidateUnusedForUser(userId, ActionTokenPurpose.PASSWORD_RESET, LocalDateTime.now(ZoneOffset.UTC));

        UUID eventId = UUID.randomUUID();
        notices.enqueue(eventId, userId, PasswordChangedV1.EVENT_TYPE, new PasswordChangedV1(eventId,
                PasswordChangedV1.EVENT_TYPE, Instant.now(), userId, user.getEmail().value(), user.getFullName()));
        authAudit.record("PASSWORD_RESET", userId, null, Map.of("method", "ADMIN", "actorId", command.adminId()));
        authMetrics.record("auth.password", "event", "reset");
        authMetrics.record("auth.session.revoked", "reason", "PASSWORD_RESET");
    }
}
