package com.group01.user.application.usecase;

import com.group01.user.application.result.AuthTokenResult;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.AuthenticationFailedException;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @Test
    void issuesTokensForActiveUserWithValidPassword() {
        LoginUseCase useCase = new LoginUseCase(userRepository, passwordEncoder, authTokenIssuer);
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(new Email("john@example.com"))
                .passwordHash("hash")
                .fullName("John Doe")
                .status(UserStatus.ACTIVE)
                .build();
        AuthTokenResult tokens = new AuthTokenResult("access", "refresh", 3600, 604800);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(authTokenIssuer.issue(user)).thenReturn(tokens);

        AuthTokenResult result = useCase.execute("John@Example.com", "secret123");

        assertThat(result).isEqualTo(tokens);
        verify(authTokenIssuer).issue(user);
    }

    @Test
    void rejectsInvalidPassword() {
        LoginUseCase useCase = new LoginUseCase(userRepository, passwordEncoder, authTokenIssuer);
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(new Email("john@example.com"))
                .passwordHash("hash")
                .fullName("John Doe")
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("john@example.com", "bad"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Email hoặc mật khẩu không đúng");
    }
}
