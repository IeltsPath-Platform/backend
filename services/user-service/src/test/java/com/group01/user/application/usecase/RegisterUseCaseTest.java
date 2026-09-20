package com.group01.user.application.usecase;

import com.group01.user.application.command.CreateUserCommand;
import com.group01.user.application.command.RegisterCommand;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseTest {

    @Mock
    private CreateUserUseCase createUserUseCase;

    @Test
    void publicRegistrationCreatesCustomerUser() {
        User user = user();
        when(createUserUseCase.execute(any(CreateUserCommand.class))).thenReturn(user);
        RegisterUseCase useCase = new RegisterUseCase(createUserUseCase);

        User result = useCase.execute(new RegisterCommand(
                "learner@example.com",
                "secret123",
                "Learner One",
                "0123456789",
                null
        ));

        ArgumentCaptor<CreateUserCommand> createCaptor = ArgumentCaptor.forClass(CreateUserCommand.class);
        verify(createUserUseCase).execute(createCaptor.capture());

        assertThat(createCaptor.getValue().roles()).containsExactly("CUSTOMER");
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void publicAdminRegistrationIsRejectedWithoutCreatingUser() {
        RegisterUseCase useCase = new RegisterUseCase(createUserUseCase);

        assertThatThrownBy(() -> useCase.execute(new RegisterCommand(
                        "admin@example.com",
                        "secret123",
                        "Admin One",
                        "0987654321",
                        "ADMIN"
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Public registration only supports the CUSTOMER role");

        verifyNoInteractions(createUserUseCase);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email(new Email("user@example.com"))
                .fullName("User")
                .status(UserStatus.ACTIVE)
                .build();
    }
}
