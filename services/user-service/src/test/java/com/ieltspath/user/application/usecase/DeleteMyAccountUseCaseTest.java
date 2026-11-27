package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.UserDeletedV1;
import com.ieltspath.user.application.exception.AccountDeletionConfirmationException;
import com.ieltspath.user.application.exception.GoogleIdTokenInvalidException;
import com.ieltspath.user.application.exception.ReauthAccountMismatchException;
import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import com.ieltspath.user.application.port.AccountDeletionCleanupPort;
import com.ieltspath.user.application.port.GoogleIdTokenVerifier;
import com.ieltspath.user.application.port.GoogleNonceStore;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.result.GoogleIdentity;
import com.ieltspath.user.config.GoogleAuthProperties;
import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.aggregate.PasswordChangeAttempts;
import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.PasswordChangeAttemptRepository;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.domain.vo.PhoneNumber;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeleteMyAccountUseCaseTest {

    private UserRepository users;
    private UserLock userLock;
    private RefreshTokenRepository refreshTokens;
    private AccessTokenRevocationStore accessTokens;
    private OAuthIdentityRepository identities;
    private PasswordEncoder encoder;
    private PasswordChangeAttemptRepository attemptsRepository;
    private GoogleIdTokenVerifier googleVerifier;
    private GoogleNonceStore nonceStore;
    private GoogleAuthProperties googleProperties;
    private AccountDeletionCleanupPort cleanupPort;
    private UserNoticeOutbox outbox;
    private DeleteMyAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        userLock = mock(UserLock.class);
        refreshTokens = mock(RefreshTokenRepository.class);
        accessTokens = mock(AccessTokenRevocationStore.class);
        identities = mock(OAuthIdentityRepository.class);
        encoder = mock(PasswordEncoder.class);
        attemptsRepository = mock(PasswordChangeAttemptRepository.class);
        googleVerifier = mock(GoogleIdTokenVerifier.class);
        nonceStore = mock(GoogleNonceStore.class);
        googleProperties = new GoogleAuthProperties();
        cleanupPort = mock(AccountDeletionCleanupPort.class);
        outbox = mock(UserNoticeOutbox.class);
        when(users.lockActiveAdminUserIds()).thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));

        useCase = new DeleteMyAccountUseCase(
                users, userLock, refreshTokens, accessTokens, identities, encoder,
                attemptsRepository, googleVerifier, nonceStore, googleProperties,
                cleanupPort, outbox);
    }

    @Test
    void deletesPasswordAccountAndEnqueuesEvent() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("learner@example.com"))
                .passwordHash("hashed")
                .fullName("Nguyen Van A")
                .phoneNumber(new PhoneNumber("0901234567"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().id(UUID.randomUUID()).name(RoleName.CUSTOMER).build()))
                .build();

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(attemptsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(encoder.matches("Secret123!", "hashed")).thenReturn(true);
        when(cleanupPort.anonymizeProfileAndDeleteIdentityData(userId)).thenReturn(Optional.empty());

        useCase.execute(userId, "Secret123!", null);

        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getEmail().value()).isEqualTo("deleted+" + userId + "@deleted.local");
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        verify(users).save(user);
        verify(refreshTokens).revokeAllActiveForUser(eq(userId), any(LocalDateTime.class), eq("ACCOUNT_DELETED"));
        verify(outbox).enqueue(any(UUID.class), eq(userId), eq(UserDeletedV1.EVENT_TYPE), any(UserDeletedV1.class));
        verify(accessTokens).revokeUser(eq(userId), any(Instant.class));
    }

    @Test
    void rejectsWrongPasswordWithVietnameseMessage() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("learner@example.com"))
                .passwordHash("hashed")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of())
                .build();

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(attemptsRepository.findByUserId(userId))
                .thenReturn(Optional.of(PasswordChangeAttempts.fresh(userId, LocalDateTime.now())));
        when(encoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(userId, "wrong", null))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessage("Mật khẩu hiện tại không chính xác.");
        verifyNoInteractions(outbox);
    }

    @Test
    void deletesGoogleOnlyAccountWithValidIdToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("gg@example.com"))
                .passwordHash(null)
                .fullName("Google User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of())
                .build();

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(googleVerifier.verify("id-tok")).thenReturn(new GoogleIdentity("sub-1", "gg@example.com", "Google User", null, null));
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE))
                .thenReturn(Optional.of(OAuthIdentity.builder()
                        .userId(userId).provider(OAuthProvider.GOOGLE).providerSubject("sub-1")
                        .linkedAt(LocalDateTime.now()).build()));
        when(cleanupPort.anonymizeProfileAndDeleteIdentityData(userId)).thenReturn(Optional.empty());

        useCase.execute(userId, null, "id-tok");

        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(outbox).enqueue(any(UUID.class), eq(userId), eq(UserDeletedV1.EVENT_TYPE), any(UserDeletedV1.class));
    }

    @Test
    void rejectsInvalidGoogleIdTokenAndMismatch() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("gg@example.com"))
                .passwordHash(null)
                .fullName("Google User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of())
                .build();

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(googleVerifier.verify("bad-tok")).thenThrow(new AuthenticationFailedException("invalid"));

        assertThatThrownBy(() -> useCase.execute(userId, null, "bad-tok"))
                .isInstanceOf(GoogleIdTokenInvalidException.class);

        when(googleVerifier.verify("other-tok")).thenReturn(new GoogleIdentity("sub-other", "gg@example.com", "Other", null, null));
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE))
                .thenReturn(Optional.of(OAuthIdentity.builder()
                        .userId(userId).provider(OAuthProvider.GOOGLE).providerSubject("sub-1")
                        .linkedAt(LocalDateTime.now()).build()));

        assertThatThrownBy(() -> useCase.execute(userId, null, "other-tok"))
                .isInstanceOf(ReauthAccountMismatchException.class);
    }

    @Test
    void idempotentWhenAlreadyDeleted() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("deleted+" + userId + "@deleted.local"))
                .fullName("Người dùng đã xóa")
                .status(UserStatus.DELETED)
                .roles(Set.of())
                .build();
        when(users.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute(userId, null, null);

        verifyNoInteractions(outbox, refreshTokens, cleanupPort, accessTokens);
    }

    @Test
    void blocksMissingConfirmationAndLastAdmin() {
        UUID adminId = UUID.randomUUID();
        User admin = User.builder()
                .id(adminId)
                .email(new Email("admin@example.com"))
                .passwordHash("hashed")
                .fullName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().id(UUID.randomUUID()).name(RoleName.ADMIN).build()))
                .build();
        when(users.lockActiveAdminUserIds()).thenReturn(List.of(adminId));
        when(users.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> useCase.execute(adminId, "Secret123!", null))
                .isInstanceOf(UserManagementRuleViolationException.class)
                .extracting("code").isEqualTo("STAFF_ACCOUNT_SELF_DELETION");

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("u@example.com"))
                .passwordHash("hashed")
                .fullName("User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of())
                .build();
        when(users.lockActiveAdminUserIds()).thenReturn(List.of(adminId));
        when(users.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute(userId, "", null))
                .isInstanceOf(AccountDeletionConfirmationException.class);
    }

    @Test
    void refusesStaffAccountsWithAStableCode() {
        UUID examinerId = UUID.randomUUID();
        User examiner = User.builder()
                .id(examinerId)
                .email(new Email("examiner@example.com"))
                .passwordHash("hashed")
                .fullName("Examiner")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().id(UUID.randomUUID()).name(RoleName.EXAMINER).build()))
                .build();
        when(users.findById(examinerId)).thenReturn(Optional.of(examiner));

        assertThatThrownBy(() -> useCase.execute(examinerId, "Secret123!", null))
                .isInstanceOf(UserManagementRuleViolationException.class)
                .hasMessageContaining("Tài khoản nhân sự không tự xóa được")
                .extracting("code").isEqualTo("STAFF_ACCOUNT_SELF_DELETION");
        assertThat(examiner.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verifyNoInteractions(outbox, cleanupPort);
    }

    @Test
    void queuesTheOwnedAvatarForDeletionInsteadOfDroppingIt() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("learner@example.com"))
                .passwordHash("hashed")
                .fullName("Learner")
                .status(UserStatus.ACTIVE)
                .roles(Set.of())
                .build();
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(attemptsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(encoder.matches("Secret123!", "hashed")).thenReturn(true);
        org.springframework.test.util.ReflectionTestUtils.setField(useCase, "projectUrl", "https://project.supabase.co");
        String key = "images/" + userId + "/" + UUID.randomUUID() + ".png";
        when(cleanupPort.anonymizeProfileAndDeleteIdentityData(userId))
                .thenReturn(Optional.of("https://project.supabase.co/storage/v1/object/public/avatars/" + key));

        useCase.execute(userId, "Secret123!", null);

        verify(cleanupPort).queueAvatarDeletion(userId, key);
    }
}