package com.group01.user.application.usecase;

import com.group01.user.application.command.ForgotPasswordCommand;
import com.group01.user.application.command.ResetPasswordCommand;
import com.group01.user.domain.aggregate.AccountActionToken;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.InvalidActionTokenException;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.repository.AccountActionTokenRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.ActionTokenPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionTokenUseCaseTest {
    @Mock private UserRepository userRepository;
    @Mock private AccountActionTokenRepository accountActionTokenRepository;
    @Mock private TokenHashService tokenHashService;
    @Mock private PasswordEncoder passwordEncoder;

    private ForgotPasswordUseCase forgotPasswordUseCase;
    private ResetPasswordUseCase resetPasswordUseCase;
    private GetActionTokensByUserIdUseCase getActionTokensByUserIdUseCase;
    private RevokeUserActionTokensUseCase revokeUserActionTokensUseCase;

    @BeforeEach
    void setUp() {
        forgotPasswordUseCase = new ForgotPasswordUseCase(userRepository, accountActionTokenRepository, tokenHashService);
        resetPasswordUseCase = new ResetPasswordUseCase(userRepository, accountActionTokenRepository, tokenHashService, passwordEncoder);
        getActionTokensByUserIdUseCase = new GetActionTokensByUserIdUseCase(accountActionTokenRepository);
        revokeUserActionTokensUseCase = new RevokeUserActionTokensUseCase(accountActionTokenRepository);
    }

    @Test
    void forgotPasswordCreatesAndSavesToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(tokenHashService.hash(any(String.class))).thenReturn("hashed-token");

        String rawToken = forgotPasswordUseCase.execute(new ForgotPasswordCommand("test@example.com"));
        assertNotNull(rawToken);

        ArgumentCaptor<AccountActionToken> captor = ArgumentCaptor.forClass(AccountActionToken.class);
        verify(accountActionTokenRepository).save(captor.capture());

        AccountActionToken saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(ActionTokenPurpose.PASSWORD_RESET, saved.getPurpose());
        assertEquals("hashed-token", saved.getTokenHash());
    }

    @Test
    void forgotPasswordThrowsWhenUserNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> forgotPasswordUseCase.execute(new ForgotPasswordCommand("notfound@example.com")));
    }

    @Test
    void resetPasswordUpdatesPasswordAndUsesToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).passwordHash("old-hash").build();
        AccountActionToken token = AccountActionToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .purpose(ActionTokenPurpose.PASSWORD_RESET)
                .tokenHash("token-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(tokenHashService.hash("my-token")).thenReturn("token-hash");
        when(accountActionTokenRepository.findByTokenHashAndPurpose("token-hash", ActionTokenPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");

        resetPasswordUseCase.execute(new ResetPasswordCommand("my-token", "new-secret"));

        assertTrue(token.isUsed());
        assertEquals("new-hash", user.getPasswordHash());
        verify(accountActionTokenRepository).save(token);
        verify(userRepository).save(user);
    }

    @Test
    void resetPasswordThrowsWhenTokenExpired() {
        AccountActionToken expiredToken = AccountActionToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .purpose(ActionTokenPurpose.PASSWORD_RESET)
                .tokenHash("token-hash")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(tokenHashService.hash("expired-token")).thenReturn("token-hash");
        when(accountActionTokenRepository.findByTokenHashAndPurpose("token-hash", ActionTokenPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidActionTokenException.class,
                () -> resetPasswordUseCase.execute(new ResetPasswordCommand("expired-token", "new-secret")));
    }

    @Test
    void revokeUserActionTokensDeletesByUserId() {
        UUID userId = UUID.randomUUID();
        revokeUserActionTokensUseCase.execute(userId);
        verify(accountActionTokenRepository).deleteByUserId(userId);
    }
}

