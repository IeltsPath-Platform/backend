package com.ieltspath.user.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ieltspath.user.domain.exception.PhoneAlreadyExistsException;
import com.ieltspath.user.application.command.UpdateUserCommand;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.PhoneNumber;
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
                .phoneNumber(new PhoneNumber("0111111111"))
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.existsByPhoneNumberAndIdNot("0222222222", userId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = useCase.execute(new UpdateUserCommand(userId, "John Updated", "0222222222"));

        assertThat(updated.getFullName()).isEqualTo("John Updated");
        assertThat(updated.getPhoneNumber().value()).isEqualTo("0222222222");
    }

    private User existingUser(UUID userId) {
        return User.builder().id(userId).email(new Email("john@example.com")).fullName("John Doe")
                .phoneNumber(new PhoneNumber("0111111111")).status(UserStatus.ACTIVE).build();
    }

    @Test
    void checksAndSavesThePhoneNumberWithoutSeparators() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser(userId)));
        when(userRepository.existsByPhoneNumberAndIdNot("0123456789", userId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = useCase.execute(new UpdateUserCommand(userId, "John Doe", "0123 456 789"));

        assertThat(updated.getPhoneNumber().value()).isEqualTo("0123456789");
    }

    @Test
    void rejectsANameOverTheColumnEvenWhenTheRequestWasNotValidated() {
        UUID userId = UUID.randomUUID();
        User existing = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute(new UpdateUserCommand(userId, "n".repeat(151), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Họ tên không được vượt quá 150 ký tự");
        assertThat(existing.getFullName()).isEqualTo("John Doe");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsAPhoneNumberUsedByAnotherAccount() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser(userId)));
        when(userRepository.existsByPhoneNumberAndIdNot("0222222222", userId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateUserCommand(userId, "John Doe", "0222222222")))
                .isInstanceOf(PhoneAlreadyExistsException.class)
                .hasMessage("Số điện thoại đã tồn tại: 0222222222");
        verify(userRepository, never()).save(any(User.class));
    }
}
