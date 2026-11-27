package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.ChangeUserStatusCommand;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.InvalidUserStatusException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import com.ieltspath.user.application.port.AuthMetrics;

@Service
@RequiredArgsConstructor
public class ChangeUserStatusUseCase {
    private final UserRepository userRepository;
    private final UserLock userLock;
    private final RefreshTokenRepository refreshTokens;
    private final AccessTokenRevocationStore accessTokens;
    private AuthAuditSupport authAudit;
    private AuthMetrics authMetrics;

    @Autowired
    void setAuthAudit(AuthAuditSupport authAudit) { this.authAudit = authAudit; }

    @Autowired
    void setAuthMetrics(AuthMetrics authMetrics) { this.authMetrics = authMetrics; }

    @Transactional
    public User execute(ChangeUserStatusCommand command) {
        if (command.status() == null) {
            throw new InvalidUserStatusException("Trạng thái người dùng không được để trống");
        }
        var activeAdminIds = userRepository.lockActiveAdminUserIds();
        // Lock before the first read; a copy read earlier stays cached and could overwrite a concurrent deletion.
        userLock.lock(command.userId());
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + command.userId()));
        if (command.userId().equals(command.actorId()) && command.status() != UserStatus.ACTIVE) {
            throw new UserManagementRuleViolationException(
                    "SELF_ACCOUNT_CHANGE", "Quản trị viên không thể tự đổi trạng thái tài khoản của mình.");
        }
        boolean activeAdmin = user.getStatus() == UserStatus.ACTIVE
                && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        boolean disablesAdmin = command.status() == UserStatus.LOCKED || command.status() == UserStatus.INACTIVE;
        if (activeAdmin && disablesAdmin && activeAdminIds.size() <= 1) {
            throw new UserManagementRuleViolationException("LAST_ADMIN", "Không thể vô hiệu hóa quản trị viên đang hoạt động cuối cùng.");
        }
        user.changeStatus(command.status());
        User saved = userRepository.save(user);
        if (command.status() != UserStatus.ACTIVE) {
            refreshTokens.revokeAllActiveForUser(user.getId(), LocalDateTime.now(), "ADMIN");
            accessTokens.revokeUser(user.getId(), Instant.now());
        }
        if (authAudit != null) authAudit.record("ACCOUNT_STATUS_CHANGED", user.getId(), null,
                java.util.Map.of("reason", command.status().name()));
        if (authMetrics != null && command.status() != UserStatus.ACTIVE) {
            authMetrics.record("auth.session.revoked", "reason", "ADMIN");
        }
        return saved;
    }
}
