package com.group01.user.application.usecase;

import com.group01.user.application.command.ChangeUserStatusCommand;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.UserStatus;
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
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = useCase.execute(new ChangeUserStatusCommand(userId, UserStatus.LOCKED));

        assertThat(updated.getStatus()).isEqualTo(UserStatus.LOCKED);
    }
}
