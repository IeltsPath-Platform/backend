package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.CreateUserCommand;
import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.EmailAlreadyExistsException;
import com.ieltspath.user.domain.exception.PhoneAlreadyExistsException;
import com.ieltspath.user.domain.exception.RoleNotFoundException;
import com.ieltspath.user.domain.repository.RoleRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.PhoneNumber;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateUserUseCase {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User execute(CreateUserCommand command) {
        log.info("Create local user requested roleCount={}", command.roles() == null ? 0 : command.roles().size());
        Email email = new Email(command.email());
        PhoneNumber phoneNumber = new PhoneNumber(command.phoneNumber());
        if (userRepository.existsByEmail(email.value())) {
            throw new EmailAlreadyExistsException("Email đã tồn tại: " + email.value());
        }
        if (phoneNumber.value() != null && userRepository.existsByPhoneNumber(phoneNumber.value())) {
            throw new PhoneAlreadyExistsException("Số điện thoại đã tồn tại: " + phoneNumber.value());
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(requirePassword(command.password())))
                .fullName(requireFullName(command.fullName()))
                .phoneNumber(phoneNumber)
                .status(UserStatus.ACTIVE)
                .roles(resolveRoles(command.roles()))
                .build();
        User savedUser = userRepository.save(user);
        log.info("Create local user completed userId={} status={}", savedUser.getId(), savedUser.getStatus());
        return savedUser;
    }

    private Set<Role> resolveRoles(Set<String> requestedRoles) {
        Set<String> roleNames = requestedRoles == null || requestedRoles.isEmpty()
                ? Set.of(RoleName.CUSTOMER.name())
                : requestedRoles.stream().map(role -> RoleName.from(role).name()).collect(Collectors.toSet());
        Set<Role> roles = new HashSet<>(roleRepository.findByNames(roleNames));
        if (roles.size() != roleNames.size()) {
            throw new RoleNotFoundException("Một hoặc nhiều vai trò không tồn tại");
        }
        return roles;
    }

    private String requireFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }
        return fullName.trim();
    }

    private String requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }
        return password;
    }
}
