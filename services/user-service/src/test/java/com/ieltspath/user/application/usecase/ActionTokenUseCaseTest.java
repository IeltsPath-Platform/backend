package com.ieltspath.user.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.user.application.command.ForgotPasswordCommand;
import com.ieltspath.user.application.command.ResetPasswordCommand;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.events.user.PasswordResetGoogleOnlyV1;
import com.ieltspath.user.domain.aggregate.AccountActionToken;
import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.InvalidActionTokenException;
import com.ieltspath.user.domain.repository.AccountActionTokenRepository;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.OutboxEventRepository;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionTokenUseCaseTest {
    @Mock private UserRepository userRepository;
    @Mock private AccountActionTokenRepository accountActionTokenRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private TokenHashService tokenHashService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserLock userLock;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserNoticeOutbox notices;
    @Mock private OAuthIdentityRepository oauthIdentityRepository;

    private ForgotPasswordUseCase forgotPasswordUseCase;
    private ResetPasswordUseCase resetPasswordUseCase;
    private GetActionTokensByUserIdUseCase getActionTokensByUserIdUseCase;
    private RevokeUserActionTokensUseCase revokeUserActionTokensUseCase;

    @BeforeEach
    void setUp() {
        forgotPasswordUseCase = new ForgotPasswordUseCase(userRepository, accountActionTokenRepository,
                outboxEventRepository, tokenHashService, userLock, new ObjectMapper().findAndRegisterModules(),
                oauthIdentityRepository, notices);
        resetPasswordUseCase = new ResetPasswordUseCase(userRepository, accountActionTokenRepository, tokenHashService,
                passwordEncoder, userLock, refreshTokenRepository, notices);
        getActionTokensByUserIdUseCase = new GetActionTokensByUserIdUseCase(accountActionTokenRepository);
        revokeUserActionTokensUseCase = new RevokeUserActionTokensUseCase(accountActionTokenRepository);
    }

    @Test
    void forgotPasswordCreatesHashedTokenAndOutboxEventForActiveUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email(new Email("test@example.com")).fullName("Test User")
                .status(UserStatus.ACTIVE).build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountActionTokenRepository.findLatestCreatedAt(userId, ActionTokenPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());
        when(tokenHashService.hash(any(String.class))).thenReturn("hashed-token");

        forgotPasswordUseCase.execute(new ForgotPasswordCommand("test@example.com"));

        ArgumentCaptor<AccountActionToken> captor = ArgumentCaptor.forClass(AccountActionToken.class);
        verify(accountActionTokenRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals(ActionTokenPurpose.PASSWORD_RESET, captor.getValue().getPurpose());
        assertEquals("hashed-token", captor.getValue().getTokenHash());
        verify(userLock).lock(userId);
        verify(accountActionTokenRepository).invalidateUnusedForUser(eq(userId), eq(ActionTokenPurpose.PASSWORD_RESET), any(LocalDateTime.class));
        verify(outboxEventRepository).save(any());
    }


    private OAuthIdentity googleIdentityOf(UUID userId) {
        return OAuthIdentity.builder().userId(userId).provider(OAuthProvider.GOOGLE).providerSubject("sub-1")
                .linkedAt(LocalDateTime.now()).build();
    }

    @Test
    void forgotPasswordForAGoogleOnlyAccountSendsAGoogleNoticeInsteadOfAResetLink() {
        UUID userId = UUID.randomUUID();
        User googleOnly = User.builder().id(userId).email(new Email("g@gmail.com")).fullName("Gia Hân")
                .status(UserStatus.ACTIVE).build();
        when(userRepository.findByEmail("g@gmail.com")).thenReturn(Optional.of(googleOnly));
        when(userRepository.findById(userId)).thenReturn(Optional.of(googleOnly));
        when(oauthIdentityRepository.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE))
                .thenReturn(Optional.of(googleIdentityOf(userId)));

        forgotPasswordUseCase.execute(new ForgotPasswordCommand("g@gmail.com"));

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(notices).enqueue(any(UUID.class), eq(userId), eq(PasswordResetGoogleOnlyV1.EVENT_TYPE), event.capture());
        PasswordResetGoogleOnlyV1 sent = (PasswordResetGoogleOnlyV1) event.getValue();
        assertEquals("g@gmail.com", sent.email());
        assertEquals("Gia Hân", sent.displayName());
        verify(accountActionTokenRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void anAccountWhosePasswordWasClearedWhenGoogleWasLinkedStillGetsAResetLink() {
        UUID userId = UUID.randomUUID();
        User cleared = User.builder().id(userId).email(new Email("c@gmail.com")).passwordHash("old")
                .status(UserStatus.ACTIVE).build();
        cleared.clearPassword();
        when(userRepository.findByEmail("c@gmail.com")).thenReturn(Optional.of(cleared));
        when(userRepository.findById(userId)).thenReturn(Optional.of(cleared));
        when(accountActionTokenRepository.findLatestCreatedAt(userId, ActionTokenPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());
        when(tokenHashService.hash(any(String.class))).thenReturn("hashed-token");

        forgotPasswordUseCase.execute(new ForgotPasswordCommand("c@gmail.com"));

        verify(accountActionTokenRepository).save(any());
        verify(outboxEventRepository).save(any());
        verify(notices, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void anAccountWithAPasswordAndGoogleLinkedStillGetsAResetLink() {
        UUID userId = UUID.randomUUID();
        User both = User.builder().id(userId).email(new Email("b@gmail.com")).passwordHash("hash")
                .status(UserStatus.ACTIVE).build();
        when(userRepository.findByEmail("b@gmail.com")).thenReturn(Optional.of(both));
        when(userRepository.findById(userId)).thenReturn(Optional.of(both));
        when(accountActionTokenRepository.findLatestCreatedAt(userId, ActionTokenPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());
        when(tokenHashService.hash(any(String.class))).thenReturn("hashed-token");

        forgotPasswordUseCase.execute(new ForgotPasswordCommand("b@gmail.com"));

        verify(accountActionTokenRepository).save(any());
        verify(notices, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void forgotPasswordDoesNotCreateDataForUnknownOrInvalidEmail() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        forgotPasswordUseCase.execute(new ForgotPasswordCommand("notfound@example.com"));
        forgotPasswordUseCase.execute(new ForgotPasswordCommand("invalid"));
        verify(accountActionTokenRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void forgotPasswordHonorsPerUserCooldown() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email(new Email("test@example.com")).status(UserStatus.ACTIVE).build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountActionTokenRepository.findLatestCreatedAt(userId, ActionTokenPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(30)));
        forgotPasswordUseCase.execute(new ForgotPasswordCommand("test@example.com"));
        verify(accountActionTokenRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void resetPasswordUpdatesPasswordAndUsesToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email(new Email("test@example.com")).passwordHash("old-hash").status(UserStatus.ACTIVE).build();
        AccountActionToken token = AccountActionToken.builder().id(UUID.randomUUID()).userId(userId)
                .purpose(ActionTokenPurpose.PASSWORD_RESET).tokenHash("token-hash")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10)).build();
        when(tokenHashService.hash("my-token")).thenReturn("token-hash");
        when(accountActionTokenRepository.findByTokenHashAndPurpose("token-hash", ActionTokenPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(accountActionTokenRepository.consumePasswordResetToken(eq("token-hash"), any(LocalDateTime.class))).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");

        resetPasswordUseCase.execute(new ResetPasswordCommand("my-token", "new-secret"));
        assertEquals("new-hash", user.getPasswordHash());
        verify(accountActionTokenRepository).consumePasswordResetToken(eq("token-hash"), any(LocalDateTime.class));
        verify(userRepository).save(user);
        verify(userLock).lock(userId);
        verify(refreshTokenRepository).revokeAllActiveForUser(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        AccountActionToken expiredToken = AccountActionToken.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                .purpose(ActionTokenPurpose.PASSWORD_RESET).tokenHash("token-hash")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5)).build();
        when(tokenHashService.hash("expired-token")).thenReturn("token-hash");
        when(accountActionTokenRepository.findByTokenHashAndPurpose("token-hash", ActionTokenPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(expiredToken));
        assertThrows(InvalidActionTokenException.class,
                () -> resetPasswordUseCase.execute(new ResetPasswordCommand("expired-token", "new-secret")));
    }

    @Test
    void resetPasswordRejectsInactiveAccountWithoutConsumingToken() {
        UUID userId = UUID.randomUUID();
        AccountActionToken token = AccountActionToken.builder().id(UUID.randomUUID()).userId(userId)
                .purpose(ActionTokenPurpose.PASSWORD_RESET).tokenHash("token-hash")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10)).build();
        when(tokenHashService.hash("my-token")).thenReturn("token-hash");
        when(accountActionTokenRepository.findByTokenHashAndPurpose("token-hash", ActionTokenPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).status(UserStatus.LOCKED).build()));
        assertThrows(InvalidActionTokenException.class,
                () -> resetPasswordUseCase.execute(new ResetPasswordCommand("my-token", "new-secret")));
        verify(accountActionTokenRepository, never()).consumePasswordResetToken(any(), any());
    }

    @Test
    void aResetTokenIssuedOnAServerOutsideUtcIsStillValidWhenItIsUsed() {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        try {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).email(new Email("test@example.com")).fullName("Test User")
                    .passwordHash("old-hash").status(UserStatus.ACTIVE).build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(accountActionTokenRepository.findLatestCreatedAt(userId, ActionTokenPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());
            when(tokenHashService.hash(any(String.class))).thenReturn("hashed-token");
            forgotPasswordUseCase.execute(new ForgotPasswordCommand("test@example.com"));
            ArgumentCaptor<AccountActionToken> saved = ArgumentCaptor.forClass(AccountActionToken.class);
            verify(accountActionTokenRepository).save(saved.capture());
            AccountActionToken issued = saved.getValue();

            when(accountActionTokenRepository.findByTokenHashAndPurpose("hashed-token", ActionTokenPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(issued));
            when(accountActionTokenRepository.consumePasswordResetToken(eq("hashed-token"), any(LocalDateTime.class))).thenReturn(true);
            when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");

            resetPasswordUseCase.execute(new ResetPasswordCommand("raw-token", "new-secret"));

            ArgumentCaptor<LocalDateTime> consumedAt = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(accountActionTokenRepository).consumePasswordResetToken(eq("hashed-token"), consumedAt.capture());
            // The repository query keeps a token only while expiresAt is after the time it is consumed.
            assertTrue(issued.getExpiresAt().isAfter(consumedAt.getValue()), "a fresh token must not look expired");
            assertTrue(issued.isValid());
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void revokeUserActionTokensDeletesByUserId() {
        UUID userId = UUID.randomUUID();
        revokeUserActionTokensUseCase.execute(userId);
        verify(accountActionTokenRepository).deleteByUserId(userId);
    }
}
