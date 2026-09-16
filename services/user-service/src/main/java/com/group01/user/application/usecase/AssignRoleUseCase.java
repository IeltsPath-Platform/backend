package com.group01.user.application.usecase;

import com.group01.user.application.command.AssignRoleCommand;
import com.group01.user.domain.aggregate.Role;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.RoleNotFoundException;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.repository.RoleRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignRoleUseCase {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public User execute(AssignRoleCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + command.userId()));
        if (command.roles() == null || command.roles().isEmpty()) {
            throw new RoleNotFoundException("Cần chọn ít nhất một vai trò");
        }
        Set<String> names = command.roles().stream()
                .map(role -> RoleName.from(role).name())
                .collect(Collectors.toSet());
        Set<Role> roles = new HashSet<>(roleRepository.findByNames(names));
        if (roles.size() != names.size()) {
            throw new RoleNotFoundException("Một hoặc nhiều vai trò không tồn tại");
        }
        user.assignRoles(roles);
        return userRepository.save(user);
    }
}
