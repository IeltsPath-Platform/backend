package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.LoginFailureReason;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditedLoginUseCaseTest {
    private static final LoginContext CONTEXT = new LoginContext("203.0.113.9", "Mozilla/5.0");

    @Mock
    private LoginUseCase loginUseCase;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LoginHistoryRecorder recorder;
    @InjectMocks
    private AuditedLoginUseCase useCase;

    @Test
    void recordsSuccessfulSignIn() {
        User user = user(UserStatus.ACTIVE);
        AuthTokenResult tokens = new AuthTokenResult("access", "refresh", 3600, 604800);
        when(loginUseCase.execute("John@Example.com", "secret")).thenReturn(tokens);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        assertThat(useCase.execute("John@Example.com", "secret", CONTEXT)).isEqualTo(tokens);

        verify(recorder).recordSuccess(user.getId(), CONTEXT);
    }

    @Test
    void recordsWrongPasswordOfActiveAccount() {
        User user = user(UserStatus.ACTIVE);
        when(loginUseCase.execute("john@example.com", "bad")).thenThrow(new AuthenticationFailedException("x"));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute("john@example.com", "bad", CONTEXT))
                .isInstanceOf(AuthenticationFailedException.class);

        verify(recorder).recordFailure(user.getId(), LoginFailureReason.BAD_PASSWORD, CONTEXT);
    }

    @Test
    void recordsWhyAnInactiveAccountWasRefused() {
        assertRefusedBecause(UserStatus.LOCKED, LoginFailureReason.ACCOUNT_LOCKED);
        assertRefusedBecause(UserStatus.INACTIVE, LoginFailureReason.ACCOUNT_INACTIVE);
        assertRefusedBecause(UserStatus.PENDING_VERIFY, LoginFailureReason.ACCOUNT_UNVERIFIED);
    }

    @Test
    void recordsNothingForUnknownOrMalformedEmail() {
        when(loginUseCase.execute("nobody@example.com", "x")).thenThrow(new AuthenticationFailedException("x"));
        when(loginUseCase.execute("not-an-email", "x")).thenThrow(new AuthenticationFailedException("x"));
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("nobody@example.com", "x", CONTEXT))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> useCase.execute("not-an-email", "x", CONTEXT))
                .isInstanceOf(AuthenticationFailedException.class);

        verifyNoInteractions(recorder);
    }

    private void assertRefusedBecause(UserStatus status, LoginFailureReason reason) {
        User user = user(status);
        doThrow(new AuthenticationFailedException("x")).when(loginUseCase).execute("john@example.com", "secret");
        doReturn(Optional.of(user)).when(userRepository).findByEmail("john@example.com");

        assertThatThrownBy(() -> useCase.execute("john@example.com", "secret", CONTEXT))
                .isInstanceOf(AuthenticationFailedException.class);

        verify(recorder).recordFailure(user.getId(), reason, CONTEXT);
    }

    private User user(UserStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(new Email("john@example.com"))
                .passwordHash("hash")
                .fullName("John Doe")
                .status(status)
                .build();
    }
}
