package com.ieltspath.user.application.usecase;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ieltspath.user.domain.exception.InvalidUserStatusException;
import com.ieltspath.user.application.command.ChangeUserStatusCommand;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeUserStatusUseCaseTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserLock userLock;
    @InjectMocks
    private ChangeUserStatusUseCase useCase;

    @Test
    void changesUserStatus() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("user@example.com"))
                .fullName("User One")
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.lockActiveAdminUserIds()).thenReturn(java.util.List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = useCase.execute(new ChangeUserStatusCommand(userId, UserStatus.LOCKED, UUID.randomUUID()));

        assertThat(updated.getStatus()).isEqualTo(UserStatus.LOCKED);
    }

    @Test
    void rejectsAMissingStatusBeforeAnyLookup() {
        assertThatThrownBy(() -> useCase.execute(new ChangeUserStatusCommand(UUID.randomUUID(), null, UUID.randomUUID())))
                .isInstanceOf(InvalidUserStatusException.class)
                .hasMessage("Trạng thái người dùng không được để trống");
        verifyNoInteractions(userRepository);
    }
}
