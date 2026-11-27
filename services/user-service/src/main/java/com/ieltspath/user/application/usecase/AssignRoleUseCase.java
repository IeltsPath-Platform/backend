package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.AssignRoleCommand;
import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.RoleNotFoundException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.repository.RoleRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.service.RoleAssignmentPolicy;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import java.time.Instant;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import com.ieltspath.user.application.port.AuthMetrics;

@Service
@RequiredArgsConstructor
public class AssignRoleUseCase {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
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
    public User execute(AssignRoleCommand command) {
        var activeAdminIds = userRepository.lockActiveAdminUserIds();
        // Lock before the first read; a copy read earlier stays cached and could overwrite a concurrent deletion.
        userLock.lock(command.userId());
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + command.userId()));
        if (command.roles() == null || command.roles().isEmpty()) {
            throw new RoleNotFoundException("Cần chọn ít nhất một vai trò");
        }
        Set<String> names = command.roles().stream()
                .map(role -> RoleName.from(role).name())
                .collect(Collectors.toSet());
        Set<RoleName> roleNames = names.stream().map(RoleName::valueOf).collect(Collectors.toSet());
        RoleAssignmentPolicy.validate(roleNames);
        boolean currentlyAdmin = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (command.userId().equals(command.actorId()) && currentlyAdmin && !roleNames.contains(RoleName.ADMIN)) {
            throw new UserManagementRuleViolationException(
                    "SELF_ADMIN_REMOVAL", "An administrator cannot remove their own ADMIN role");
        }
        boolean activeAdmin = user.getStatus() == UserStatus.ACTIVE && currentlyAdmin;
        if (activeAdmin && !roleNames.contains(RoleName.ADMIN) && activeAdminIds.size() <= 1) {
            throw new UserManagementRuleViolationException("LAST_ADMIN", "The last active administrator cannot lose ADMIN");
        }
        Set<Role> roles = new HashSet<>(roleRepository.findByNames(names));
        if (roles.size() != names.size()) {
            throw new RoleNotFoundException("Một hoặc nhiều vai trò không tồn tại");
        }
        user.assignRoles(roles);
        User saved = userRepository.save(user);
        refreshTokens.revokeAllActiveForUser(user.getId(), LocalDateTime.now(), "ADMIN");
        accessTokens.revokeUser(user.getId(), Instant.now());
        if (authAudit != null) authAudit.record("ROLE_CHANGED", user.getId(), null,
                java.util.Map.of("reason", "ADMIN"));
        if (authMetrics != null) authMetrics.record("auth.session.revoked", "reason", "ADMIN");
        return saved;
    }
}
