package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.ChangePasswordCommand;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.repository.StudySessionRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {
    @Mock UserRepository users;
    @Mock RefreshTokenRepository tokens;
    @Mock PasswordEncoder encoder;
    @Mock StudySessionRepository studySessions;

    @Test void changesHashAndRevokesActiveRefreshTokens() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email(new Email("person@example.com")).passwordHash("old-hash")
                .fullName("Person").status(UserStatus.ACTIVE).build();
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(encoder.matches("old-password", "old-hash")).thenReturn(true);
        when(encoder.matches("next-password", "old-hash")).thenReturn(false);
        when(encoder.encode("next-password")).thenReturn("new-hash");
        new ChangePasswordUseCase(users, tokens, encoder, studySessions).execute(new ChangePasswordCommand(id, "old-password", "next-password"));
        assertEquals("new-hash", user.getPasswordHash());
        verify(tokens).revokeAllActiveForUser(eq(id), any());
    }

    @Test void wrongCurrentPasswordReturnsExplicitCode() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email(new Email("person@example.com")).passwordHash("old-hash")
                .fullName("Person").status(UserStatus.ACTIVE).build();
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(encoder.matches("wrong-password", "old-hash")).thenReturn(false);
        var error = assertThrows(com.ieltspath.user.domain.exception.PasswordChangeException.class,
                () -> new ChangePasswordUseCase(users, tokens, encoder, studySessions).execute(new ChangePasswordCommand(id, "wrong-password", "next-password")));
        assertEquals("CURRENT_PASSWORD_INVALID", error.getCode());
        verifyNoInteractions(tokens);
    }

    @Test void rateLimitsAfterFiveWrongPasswords() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email(new Email("person@example.com")).passwordHash("old-hash")
                .fullName("Person").status(UserStatus.ACTIVE).build();
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(encoder.matches("wrong-password", "old-hash")).thenReturn(false);
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(users, tokens, encoder, studySessions);
        ChangePasswordCommand command = new ChangePasswordCommand(id, "wrong-password", "next-password");
        for (int i = 0; i < 5; i++) {
            var error = assertThrows(com.ieltspath.user.domain.exception.PasswordChangeException.class,
                    () -> useCase.execute(command));
            assertEquals("CURRENT_PASSWORD_INVALID", error.getCode());
        }
        var limited = assertThrows(com.ieltspath.user.domain.exception.PasswordChangeException.class,
                () -> useCase.execute(command));
        assertEquals("PASSWORD_ATTEMPTS_LIMITED", limited.getCode());
        assertTrue(limited.isRateLimited());
        verify(users, times(5)).findById(id);
    }

    @Test void activeLockoutDoesNotExpireWhenFailureWindowEndsFirst() throws Exception {
        UUID id = UUID.randomUUID();
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(users, tokens, encoder, studySessions);
        var attemptsField = ChangePasswordUseCase.class.getDeclaredField("attempts");
        attemptsField.setAccessible(true);
        @SuppressWarnings("unchecked") var attempts = (java.util.concurrent.ConcurrentHashMap<UUID, Object>) attemptsField.get(useCase);
        Class<?> stateType = Class.forName(ChangePasswordUseCase.class.getName() + "$Attempts");
        var constructor = stateType.getDeclaredConstructor(int.class, long.class, long.class);
        constructor.setAccessible(true);
        long now = System.currentTimeMillis();
        attempts.put(id, constructor.newInstance(5, now - 16 * 60 * 1000L, now + 60_000L));

        var error = assertThrows(com.ieltspath.user.domain.exception.PasswordChangeException.class,
                () -> useCase.execute(new ChangePasswordCommand(id, "anything", "next-password")));
        assertEquals("PASSWORD_ATTEMPTS_LIMITED", error.getCode());
        verifyNoInteractions(users, tokens, encoder, studySessions);
    }
}
