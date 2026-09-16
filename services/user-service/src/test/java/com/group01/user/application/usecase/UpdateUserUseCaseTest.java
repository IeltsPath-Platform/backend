package com.group01.user.application.usecase;

import com.group01.user.application.command.UpdateUserCommand;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.PhoneNumber;
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
class UpdateUserUseCaseTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UpdateUserUseCase useCase;

    @Test
    void updatesProfileWhenUserExistsAndPhoneIsUnique() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder()
                .id(userId)
                .email(new Email("john@example.com"))
                .fullName("John Doe")
                .phoneNumber(new PhoneNumber("111"))
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.existsByPhoneNumberAndIdNot("222", userId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = useCase.execute(new UpdateUserCommand(userId, "John Updated", "222"));

        assertThat(updated.getFullName()).isEqualTo("John Updated");
        assertThat(updated.getPhoneNumber().value()).isEqualTo("222");
    }
}
