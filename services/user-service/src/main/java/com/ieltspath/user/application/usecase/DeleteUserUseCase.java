package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteUserUseCase {
    private final UserRepository userRepository;

    @Transactional
    public User execute(UUID id, UUID actorId) {
        var activeAdminIds = userRepository.lockActiveAdminUserIds();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + id));
        if (id.equals(actorId)) {
            throw new UserManagementRuleViolationException(
                    "SELF_ACCOUNT_CHANGE", "An administrator cannot deactivate their own account");
        }
        boolean activeAdmin = user.getStatus() == UserStatus.ACTIVE
                && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (activeAdmin && activeAdminIds.size() <= 1) {
            throw new UserManagementRuleViolationException("LAST_ADMIN", "The last active administrator cannot be deactivated");
        }
        user.deactivate();
        return userRepository.save(user);
    }
}
