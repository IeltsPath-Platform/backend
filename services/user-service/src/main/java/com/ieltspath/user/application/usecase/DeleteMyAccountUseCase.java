package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.UserDeletedV1;
import com.ieltspath.user.application.exception.AccountDeletionConfirmationException;
import com.ieltspath.user.application.exception.GoogleIdTokenInvalidException;
import com.ieltspath.user.application.exception.GoogleSignInUnavailableException;
import com.ieltspath.user.application.exception.ReauthAccountMismatchException;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import com.ieltspath.user.application.port.AccountDeletionCleanupPort;
import com.ieltspath.user.application.port.GoogleIdTokenVerifier;
import com.ieltspath.user.application.port.GoogleNonceStore;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.config.GoogleAuthProperties;
import com.ieltspath.user.domain.aggregate.PasswordChangeAttempts;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.PasswordChangeAttemptRepository;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteMyAccountUseCase {
    private final UserRepository users;
    private final UserLock userLock;
    private final RefreshTokenRepository refreshTokens;
    private final AccessTokenRevocationStore accessTokens;
    private final OAuthIdentityRepository identities;
    private final PasswordEncoder encoder;
    private final PasswordChangeAttemptRepository attemptsRepository;
    private final GoogleIdTokenVerifier googleVerifier;
    private final GoogleNonceStore nonceStore;
    private final GoogleAuthProperties googleProperties;
    private final AccountDeletionCleanupPort cleanupPort;
    private final UserNoticeOutbox outbox;

    @Value("${app.user.avatar.bucket:avatars}")
    private String avatarBucket = "avatars";
    @Value("${app.storage.supabase.url:}")
    private String projectUrl = "";

    private AuthAuditSupport authAudit;

    @Autowired(required = false)
    void setAuthAudit(AuthAuditSupport authAudit) {
        this.authAudit = authAudit;
    }

    @Transactional(noRollbackFor = PasswordChangeException.class)
    public void execute(UUID userId, String currentPassword, String googleIdToken) {
        var activeAdminIds = users.lockActiveAdminUserIds();
        userLock.lock(userId);
        User user = users.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        if (user.getStatus() == UserStatus.DELETED) {
            return;
        }
        // Staff accounts hold work for others (grading queues, keys, content); an administrator closes them instead.
        if (user.getRoles().stream().anyMatch(role -> role.getName() != RoleName.CUSTOMER)) {
            throw new UserManagementRuleViolationException("STAFF_ACCOUNT_SELF_DELETION",
                    "Tài khoản nhân sự không tự xóa được. Vui lòng liên hệ quản trị viên để chuyển giao công việc và xóa tài khoản.");
        }
        boolean activeAdmin = user.getStatus() == UserStatus.ACTIVE
                && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (activeAdmin && activeAdminIds.size() <= 1) {
            throw new UserManagementRuleViolationException("LAST_ADMIN",
                    "Không thể xóa quản trị viên đang hoạt động cuối cùng.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            verifyPassword(user, currentPassword, now);
        } else {
            verifyGoogle(userId, googleIdToken);
        }

        user.markDeleted(now);
        users.save(user);
        refreshTokens.revokeAllActiveForUser(userId, now, "ACCOUNT_DELETED");
        String oldAvatarRef = cleanupPort.anonymizeProfileAndDeleteIdentityData(userId).orElse(null);

        if (authAudit != null) {
            authAudit.record("ACCOUNT_DELETED", userId, null, Map.of("method",
                    blank(currentPassword) ? "GOOGLE" : "PASSWORD"));
        }

        Instant occurredAt = Instant.now();
        outbox.enqueue(UUID.randomUUID(), userId, UserDeletedV1.EVENT_TYPE,
                new UserDeletedV1(UUID.randomUUID(), UserDeletedV1.EVENT_TYPE, occurredAt, userId));
        accessTokens.revokeUser(userId, occurredAt);
        queueAvatarDeletion(userId, oldAvatarRef);
    }

    private void verifyPassword(User user, String currentPassword, LocalDateTime now) {
        PasswordChangeAttempts attempts = attemptsRepository.findByUserId(user.getId())
                .orElseGet(() -> PasswordChangeAttempts.fresh(user.getId(), now));
        if (attempts.isLocked(now)) {
            throw new PasswordChangeException("PASSWORD_ATTEMPTS_LIMITED",
                    "Đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau 15 phút.", true);
        }
        if (blank(currentPassword)) {
            throw new AccountDeletionConfirmationException("ACCOUNT_DELETION_CONFIRMATION_REQUIRED",
                    "Vui lòng nhập mật khẩu hiện tại để xác nhận xóa tài khoản.");
        }
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            attempts.recordFailure(now);
            attemptsRepository.save(attempts);
            throw new PasswordChangeException("INVALID_CURRENT_PASSWORD", "Mật khẩu hiện tại không chính xác.");
        }
        attemptsRepository.deleteByUserId(user.getId());
    }

    private void verifyGoogle(UUID userId, String googleIdToken) {
        if (blank(googleIdToken)) {
            throw new AccountDeletionConfirmationException("ACCOUNT_DELETION_CONFIRMATION_REQUIRED",
                    "Vui lòng xác thực tài khoản Google để xác nhận xóa tài khoản.");
        }
        com.ieltspath.user.application.result.GoogleIdentity identity;
        try {
            identity = googleVerifier.verify(googleIdToken);
        } catch (AuthenticationFailedException invalid) {
            throw new GoogleIdTokenInvalidException();
        }
        String nonce = identity.nonce();
        if (blank(nonce)) {
            if (googleProperties.isRequireNonce()) {
                throw new GoogleIdTokenInvalidException();
            }
        } else {
            try {
                if (!nonceStore.consume(nonce)) {
                    throw new GoogleIdTokenInvalidException();
                }
            } catch (GoogleIdTokenInvalidException rejected) {
                throw rejected;
            } catch (RuntimeException unavailable) {
                if (googleProperties.isRequireNonce()) {
                    throw new GoogleSignInUnavailableException(unavailable);
                }
            }
        }
        var linked = identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE).orElse(null);
        if (linked == null || !linked.getProviderSubject().equals(identity.subject())) {
            throw new ReauthAccountMismatchException();
        }
    }

    /** Queued in this transaction and removed by {@code AvatarDeletionJob}, which retries until storage confirms. */
    private void queueAvatarDeletion(UUID userId, String oldAvatarRef) {
        if (oldAvatarRef == null) return;
        AvatarReferences.ownedKey(oldAvatarRef, projectUrl, avatarBucket, userId)
                .ifPresent(key -> cleanupPort.queueAvatarDeletion(userId, key));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
