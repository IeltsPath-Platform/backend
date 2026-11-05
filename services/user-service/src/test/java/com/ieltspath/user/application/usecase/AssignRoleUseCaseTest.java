package com.ieltspath.user.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ieltspath.user.domain.exception.RoleNotFoundException;
import com.ieltspath.user.application.command.AssignRoleCommand;
import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.RoleRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignRoleUseCaseTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @InjectMocks
    private AssignRoleUseCase useCase;

    @Test
    void assignsExistingRolesToUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("admin@example.com"))
                .fullName("Admin One")
                .status(UserStatus.ACTIVE)
                .build();
        Role admin = Role.builder().id(UUID.randomUUID()).name(RoleName.ADMIN).build();
        when(userRepository.lockActiveAdminUserIds()).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByNames(Set.of("ADMIN"))).thenReturn(List.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = useCase.execute(new AssignRoleCommand(userId, Set.of("ADMIN"), UUID.randomUUID()));

        assertThat(updated.getRoles()).extracting(role -> role.getName().name()).containsExactly("ADMIN");
    }

    @Test
    void rejectsAnEmptyOrMissingRoleSetWithoutSaving() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("user@example.com"))
                .fullName("User One")
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.lockActiveAdminUserIds()).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        for (Set<String> roles : new Set[]{Set.of(), null}) {
            assertThatThrownBy(() -> useCase.execute(new AssignRoleCommand(userId, roles, UUID.randomUUID())))
                    .isInstanceOf(RoleNotFoundException.class)
                    .hasMessage("Cần chọn ít nhất một vai trò");
        }
        verify(userRepository, never()).save(any(User.class));
    }
}
