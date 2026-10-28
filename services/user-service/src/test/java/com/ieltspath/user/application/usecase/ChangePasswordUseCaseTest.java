package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.ChangePasswordCommand;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.PasswordChangeAttempts;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.domain.repository.PasswordChangeAttemptRepository;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {
    @Mock UserRepository users;
    @Mock RefreshTokenRepository tokens;
    @Mock PasswordEncoder encoder;
    @Mock UserLock userLock;

    private final Map<UUID, PasswordChangeAttempts> stored = new HashMap<>();
    private ChangePasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangePasswordUseCase(users, tokens, encoder, userLock, new PasswordChangeAttemptRepository() {
            @Override public Optional<PasswordChangeAttempts> findByUserId(UUID userId) { return Optional.ofNullable(stored.get(userId)); }
            @Override public void save(PasswordChangeAttempts attempts) { stored.put(attempts.getUserId(), attempts); }
            @Override public void deleteByUserId(UUID userId) { stored.remove(userId); }
        });
    }

    private User userWithHash(UUID id) {
        return User.builder().id(id).email(new Email("person@example.com")).passwordHash("old-hash")
                .fullName("Person").status(UserStatus.ACTIVE).build();
    }

    @Test void changesHashRevokesActiveRefreshTokensAndClearsTheFailureCount() {
        UUID id = UUID.randomUUID();
        User user = userWithHash(id);
        stored.put(id, new PasswordChangeAttempts(id, 3, LocalDateTime.now(), null));
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(encoder.matches("old-password", "old-hash")).thenReturn(true);
        when(encoder.matches("next-password", "old-hash")).thenReturn(false);
        when(encoder.encode("next-password")).thenReturn("new-hash");

        useCase.execute(new ChangePasswordCommand(id, "old-password", "next-password"));

        assertEquals("new-hash", user.getPasswordHash());
        verify(tokens).revokeAllActiveForUser(eq(id), any());
        assertTrue(stored.isEmpty());
    }

    @Test void locksTheUserBeforeReadingAnything() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(userWithHash(id)));
        when(encoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThrows(PasswordChangeException.class,
                () -> useCase.execute(new ChangePasswordCommand(id, "wrong-password", "next-password")));

        InOrder order = inOrder(userLock, users);
        order.verify(userLock).lock(id);
        order.verify(users).findById(id);
    }

    @Test void wrongCurrentPasswordReturnsExplicitCodeAndIsCounted() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(userWithHash(id)));
        when(encoder.matches("wrong-password", "old-hash")).thenReturn(false);

        var error = assertThrows(PasswordChangeException.class,
                () -> useCase.execute(new ChangePasswordCommand(id, "wrong-password", "next-password")));

        assertEquals("CURRENT_PASSWORD_INVALID", error.getCode());
        assertEquals(1, stored.get(id).getFailures());
        verifyNoInteractions(tokens);
    }

    @Test void rateLimitsAfterFiveWrongPasswords() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(userWithHash(id)));
        when(encoder.matches("wrong-password", "old-hash")).thenReturn(false);
        ChangePasswordCommand command = new ChangePasswordCommand(id, "wrong-password", "next-password");
        for (int i = 0; i < 5; i++) {
            var error = assertThrows(PasswordChangeException.class, () -> useCase.execute(command));
            assertEquals("CURRENT_PASSWORD_INVALID", error.getCode());
        }

        var limited = assertThrows(PasswordChangeException.class, () -> useCase.execute(command));

        assertEquals("PASSWORD_ATTEMPTS_LIMITED", limited.getCode());
        assertTrue(limited.isRateLimited());
        org.mockito.Mockito.verify(users, org.mockito.Mockito.times(5)).findById(id);
    }

    @Test void anActiveLockRejectsEvenTheRightPassword() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        stored.put(id, new PasswordChangeAttempts(id, 5, now.minusMinutes(16), now.plusMinutes(1)));

        var error = assertThrows(PasswordChangeException.class,
                () -> useCase.execute(new ChangePasswordCommand(id, "old-password", "next-password")));

        assertEquals("PASSWORD_ATTEMPTS_LIMITED", error.getCode());
        verifyNoInteractions(users, tokens, encoder);
    }

    @Test void anExpiredLockAllowsTheChangeAgain() {
        UUID id = UUID.randomUUID();
        User user = userWithHash(id);
        LocalDateTime now = LocalDateTime.now();
        stored.put(id, new PasswordChangeAttempts(id, 5, now.minusMinutes(40), now.minusMinutes(1)));
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(encoder.matches("old-password", "old-hash")).thenReturn(true);
        when(encoder.matches("next-password", "old-hash")).thenReturn(false);
        when(encoder.encode("next-password")).thenReturn("new-hash");

        useCase.execute(new ChangePasswordCommand(id, "old-password", "next-password"));

        assertEquals("new-hash", user.getPasswordHash());
    }
}
