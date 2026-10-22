package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.ChangeUserStatusCommand;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.InvalidUserStatusException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeUserStatusUseCase {
    private final UserRepository userRepository;

    @Transactional
    public User execute(ChangeUserStatusCommand command) {
        if (command.status() == null) {
            throw new InvalidUserStatusException("Trạng thái người dùng không được để trống");
        }
        var activeAdminIds = userRepository.lockActiveAdminUserIds();
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + command.userId()));
        if (command.userId().equals(command.actorId()) && command.status() != UserStatus.ACTIVE) {
            throw new UserManagementRuleViolationException(
                    "SELF_ACCOUNT_CHANGE", "An administrator cannot change their own status away from ACTIVE");
        }
        boolean activeAdmin = user.getStatus() == UserStatus.ACTIVE
                && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        boolean disablesAdmin = command.status() == UserStatus.LOCKED || command.status() == UserStatus.INACTIVE;
        if (activeAdmin && disablesAdmin && activeAdminIds.size() <= 1) {
            throw new UserManagementRuleViolationException("LAST_ADMIN", "The last active administrator cannot be deactivated");
        }
        user.changeStatus(command.status());
        return userRepository.save(user);
    }
}
