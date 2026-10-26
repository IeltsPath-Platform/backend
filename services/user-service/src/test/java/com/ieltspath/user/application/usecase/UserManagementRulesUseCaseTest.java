package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.AssignRoleCommand;
import com.ieltspath.user.application.command.ChangeUserStatusCommand;
import com.ieltspath.user.application.command.CreateUserCommand;
import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.repository.RoleRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementRulesUseCaseTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder passwordEncoder;

    private AssignRoleUseCase assignRole;
    private ChangeUserStatusUseCase changeStatus;
    private DeleteUserUseCase deleteUser;
    private CreateUserUseCase createUser;

    @BeforeEach
    void setUp() {
        assignRole = new AssignRoleUseCase(users, roles);
        changeStatus = new ChangeUserStatusUseCase(users);
        deleteUser = new DeleteUserUseCase(users);
        createUser = new CreateUserUseCase(users, roles, passwordEncoder);
    }

    @Test
    void rejectsConflictingRolesOnCreate() {
        when(users.existsByEmail("admin@example.com")).thenReturn(false);
        when(users.existsByPhoneNumber("0123456789")).thenReturn(false);

        assertRule("ROLE_CONFLICT", () -> createUser.execute(new CreateUserCommand(
                "admin@example.com", "secret123", "Admin", "0123456789", Set.of("ADMIN", "CONTENT_AUTHOR"))));

        verify(roles, never()).findByNames(any());
        verify(users, never()).save(any());
    }

    @Test
    void rejectsConflictingRolesOnAssignment() {
        UUID target = UUID.randomUUID();
        when(users.lockActiveAdminUserIds()).thenReturn(List.of());
        when(users.findById(target)).thenReturn(Optional.of(user(target, UserStatus.ACTIVE, Set.of())));

        assertRule("ROLE_CONFLICT", () -> assignRole.execute(
                new AssignRoleCommand(target, Set.of("ADMIN", "EXAMINER"), UUID.randomUUID())));

        verify(users, never()).save(any());
    }

    @Test
    void rejectsRemovingOwnAdminRole() {
        UUID adminId = UUID.randomUUID();
        when(users.lockActiveAdminUserIds()).thenReturn(List.of(adminId));
        when(users.findById(adminId)).thenReturn(Optional.of(user(adminId, UserStatus.ACTIVE, Set.of(RoleName.ADMIN))));

        assertRule("SELF_ADMIN_REMOVAL", () -> assignRole.execute(
                new AssignRoleCommand(adminId, Set.of("CUSTOMER"), adminId)));

        verify(users, never()).save(any());
    }

    @Test
    void rejectsRemovingAdminFromLastActiveAdministrator() {
        UUID adminId = UUID.randomUUID();
        when(users.lockActiveAdminUserIds()).thenReturn(List.of(adminId));
        when(users.findById(adminId)).thenReturn(Optional.of(user(adminId, UserStatus.ACTIVE, Set.of(RoleName.ADMIN))));

        assertRule("LAST_ADMIN", () -> assignRole.execute(
                new AssignRoleCommand(adminId, Set.of("CUSTOMER"), UUID.randomUUID())));

        verify(users, never()).save(any());
    }

    @Test
    void rejectsChangingOwnStatusAwayFromActive() {
        UUID adminId = UUID.randomUUID();
        when(users.lockActiveAdminUserIds()).thenReturn(List.of(adminId));
        when(users.findById(adminId)).thenReturn(Optional.of(user(adminId, UserStatus.ACTIVE, Set.of(RoleName.ADMIN))));

        assertRule("SELF_ACCOUNT_CHANGE", () -> changeStatus.execute(
                new ChangeUserStatusCommand(adminId, UserStatus.LOCKED, adminId)));

        verify(users, never()).save(any());
    }

    @Test
    void rejectsLockingLastActiveAdministrator() {
        UUID adminId = UUID.randomUUID();
        when(users.lockActiveAdminUserIds()).thenReturn(List.of(adminId));
        when(users.findById(adminId)).thenReturn(Optional.of(user(adminId, UserStatus.ACTIVE, Set.of(RoleName.ADMIN))));

        assertRule("LAST_ADMIN", () -> changeStatus.execute(
                new ChangeUserStatusCommand(adminId, UserStatus.LOCKED, UUID.randomUUID())));

        verify(users, never()).save(any());
    }

    @Test
    void allowsThePlanListedPendingVerificationStatusForLastAdministrator() {
        UUID adminId = UUID.randomUUID();
        User admin = user(adminId, UserStatus.ACTIVE, Set.of(RoleName.ADMIN));
        when(users.lockActiveAdminUserIds()).thenReturn(List.of(adminId));
        when(users.findById(adminId)).thenReturn(Optional.of(admin));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        changeStatus.execute(new ChangeUserStatusCommand(adminId, UserStatus.PENDING_VERIFY, UUID.randomUUID()));

        verify(users).save(admin);
    }

    @Test
    void rejectsDeletingSelfAndLastActiveAdministrator() {
        UUID adminId = UUID.randomUUID();
        User admin = user(adminId, UserStatus.ACTIVE, Set.of(RoleName.ADMIN));
        when(users.lockActiveAdminUserIds()).thenReturn(List.of(adminId));
        when(users.findById(adminId)).thenReturn(Optional.of(admin));

        assertRule("SELF_ACCOUNT_CHANGE", () -> deleteUser.execute(adminId, adminId));
        assertRule("LAST_ADMIN", () -> deleteUser.execute(adminId, UUID.randomUUID()));

        verify(users, never()).save(any());
    }

    private User user(UUID id, UserStatus status, Set<RoleName> roleNames) {
        Set<Role> userRoles = roleNames.stream()
                .map(name -> Role.builder().id(UUID.randomUUID()).name(name).build())
                .collect(java.util.stream.Collectors.toSet());
        return User.builder().id(id).email(new Email(id + "@example.com")).fullName("Test User")
                .status(status).roles(userRoles).build();
    }

    private void assertRule(String code, org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation)
                .isInstanceOf(UserManagementRuleViolationException.class)
                .extracting(exception -> ((UserManagementRuleViolationException) exception).getCode())
                .isEqualTo(code);
    }
}
