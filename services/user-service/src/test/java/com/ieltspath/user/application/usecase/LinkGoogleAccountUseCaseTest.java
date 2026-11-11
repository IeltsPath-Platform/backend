package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.GoogleAccountLinkedV1;
import com.ieltspath.events.user.UserEmailVerificationStateV1;
import com.ieltspath.user.application.exception.GoogleAccountConflictException;
import com.ieltspath.user.application.exception.GoogleAccountNotActiveException;
import com.ieltspath.user.application.exception.GoogleEmailNotSupportedException;
import com.ieltspath.user.application.exception.GoogleSignInNotAllowedException;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.application.result.GoogleIdentity;
import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.RoleRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkGoogleAccountUseCaseTest {
    @Mock UserRepository users;
    @Mock OAuthIdentityRepository identities;
    @Mock RoleRepository roles;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock UserLock userLock;
    @Mock AuthTokenIssuer tokenIssuer;
    @Mock UserNoticeOutbox notices;

    private LinkGoogleAccountUseCase useCase;
    private final UUID userId = UUID.randomUUID();
    private final User customerWithPassword = user(userId, UserStatus.ACTIVE, Set.of(RoleName.CUSTOMER), "hash");
    private final AuthTokenResult tokens = new AuthTokenResult("access", "refresh", 3600, 604800);

    @BeforeEach
    void setUp() {
        useCase = new LinkGoogleAccountUseCase(users, identities, roles, refreshTokens, userLock, tokenIssuer, notices);
        lenient().when(users.findById(userId)).thenReturn(Optional.of(customerWithPassword));
        lenient().when(tokenIssuer.issue(any(User.class))).thenReturn(tokens);
        lenient().when(identities.save(any(OAuthIdentity.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void linksAVerifiedPasswordAccountAndKeepsItsPassword() {
        when(users.findByEmail("learner@gmail.com")).thenReturn(Optional.of(customerWithPassword));
        when(identities.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty());
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE)).thenReturn(Optional.empty());

        var result = useCase.execute(new GoogleIdentity("google-sub", "learner@gmail.com", "Learner", null));

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.tokens()).isEqualTo(tokens);
        assertThat(customerWithPassword.getPasswordHash()).isEqualTo("hash");
        verify(userLock).lock(userId);
        verify(users, never()).clearPasswordHash(userId);
        verify(refreshTokens, never()).revokeAllActiveForUser(eq(userId), any());
        verify(notices).enqueue(any(), eq(userId), eq(GoogleAccountLinkedV1.EVENT_TYPE), any());
        verify(notices, never()).enqueue(any(), eq(userId), eq(UserEmailVerificationStateV1.EVENT_TYPE), any());
        ArgumentCaptor<OAuthIdentity> identity = ArgumentCaptor.forClass(OAuthIdentity.class);
        verify(identities).save(identity.capture());
        assertThat(identity.getValue().getProviderSubject()).isEqualTo("google-sub");
        assertThat(identity.getValue().getLastAuthenticatedAt()).isNotNull();
    }

    @Test
    void verifiesAnUnverifiedAccountAndRemovesThePasswordNobodyProved() {
        User unverified = User.builder().id(userId).email(new Email("learner@gmail.com")).passwordHash("hash")
                .fullName("Learner").status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().id(UUID.randomUUID()).name(RoleName.CUSTOMER).build())).build();
        when(users.findByEmail("learner@gmail.com")).thenReturn(Optional.of(unverified));
        when(users.findById(userId)).thenReturn(Optional.of(unverified));
        when(identities.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty());
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE)).thenReturn(Optional.empty());

        useCase.execute(new GoogleIdentity("google-sub", "learner@gmail.com", "Learner", null));

        assertThat(unverified.isEmailVerified()).isTrue();
        assertThat(unverified.getPasswordHash()).isNull();
        verify(users).clearPasswordHash(userId);
        verify(refreshTokens).revokeAllActiveForUser(eq(userId), any());
        verify(users).save(unverified);
        verify(notices).enqueue(any(), eq(userId), eq(UserEmailVerificationStateV1.EVENT_TYPE), any());
        verify(notices).enqueue(any(), eq(userId), eq(GoogleAccountLinkedV1.EVENT_TYPE), any());
    }

    @Test
    void createsNewCustomerWithoutPassword() {
        User newUser = user(userId, UserStatus.ACTIVE, Set.of(RoleName.CUSTOMER), null);
        when(users.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
        when(roles.findByNames(Set.of("CUSTOMER"))).thenReturn(List.of(Role.builder().name(RoleName.CUSTOMER).build()));
        when(users.save(any(User.class))).thenReturn(newUser);
        when(users.findById(userId)).thenReturn(Optional.of(newUser));
        when(identities.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "new-sub"))
                .thenReturn(Optional.empty());
        useCase.execute(new GoogleIdentity("new-sub", "new@gmail.com", "New Learner", null));

        ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(users).save(created.capture());
        assertThat(created.getValue().getPasswordHash()).isNull();
        assertThat(created.getValue().getRoles()).extracting(Role::getName).containsExactly(RoleName.CUSTOMER);
        verify(refreshTokens, never()).revokeAllActiveForUser(eq(userId), any());
        verify(notices).enqueue(any(), eq(userId), eq(UserEmailVerificationStateV1.EVENT_TYPE), any());
        verify(notices, never()).enqueue(any(), any(), eq(GoogleAccountLinkedV1.EVENT_TYPE), any());
    }

    @Test
    void rejectsUntrustedEmailBeforeLookingUpAnAccount() {
        assertThatThrownBy(() -> useCase.execute(new GoogleIdentity(
                "sub", "person@hotmail.com", "Person", null)))
                .isInstanceOf(GoogleEmailNotSupportedException.class);
        verify(users, never()).findByEmail(any());
    }

    @Test
    void rejectsDifferentGoogleIdentityAlreadyLinkedToEmail() {
        when(users.findByEmail("learner@gmail.com")).thenReturn(Optional.of(customerWithPassword));
        when(identities.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "new-sub"))
                .thenReturn(Optional.empty());
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE)).thenReturn(Optional.of(
                OAuthIdentity.builder().userId(userId).provider(OAuthProvider.GOOGLE).providerSubject("old-sub").build()));

        assertThatThrownBy(() -> useCase.execute(new GoogleIdentity("new-sub", "learner@gmail.com", "Learner", null)))
                .isInstanceOf(GoogleAccountConflictException.class);
        verify(users, never()).clearPasswordHash(userId);
        verify(identities, never()).save(any());
    }

    @Test
    void blocksStaffEvenWhenGoogleIdentityAlreadyExists() {
        User admin = user(userId, UserStatus.ACTIVE, Set.of(RoleName.ADMIN), "hash");
        when(identities.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.of(OAuthIdentity.builder().userId(userId).provider(OAuthProvider.GOOGLE)
                        .providerSubject("google-sub").build()));
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE)).thenReturn(Optional.of(
                OAuthIdentity.builder().userId(userId).provider(OAuthProvider.GOOGLE)
                        .providerSubject("google-sub").build()));
        when(users.findById(userId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> useCase.execute(new GoogleIdentity("google-sub", "admin@gmail.com", "Admin", null)))
                .isInstanceOf(GoogleSignInNotAllowedException.class);
        verify(users, never()).clearPasswordHash(userId);
        verify(identities, never()).save(any());
    }

    @Test
    void reportsInactiveAccountWithUserIdAndStatus() {
        User locked = user(userId, UserStatus.LOCKED, Set.of(RoleName.CUSTOMER), "hash");
        when(users.findById(userId)).thenReturn(Optional.of(locked));
        when(users.findByEmail("learner@gmail.com")).thenReturn(Optional.of(locked));
        when(identities.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty());
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new GoogleIdentity("google-sub", "learner@gmail.com", "Learner", null)))
                .isInstanceOfSatisfying(GoogleAccountNotActiveException.class, exception -> {
                    assertThat(exception.userId()).isEqualTo(userId);
                    assertThat(exception.status()).isEqualTo(UserStatus.LOCKED);
                });
        verify(users, never()).clearPasswordHash(userId);
        verify(identities, never()).save(any());
    }

    private static User user(UUID id, UserStatus status, Set<RoleName> roleNames, String passwordHash) {
        Set<Role> userRoles = roleNames.stream()
                .map(name -> Role.builder().id(UUID.randomUUID()).name(name).build())
                .collect(java.util.stream.Collectors.toSet());
        return User.builder().id(id).email(new Email("learner@gmail.com")).passwordHash(passwordHash)
                .emailVerifiedAt(java.time.LocalDateTime.now())
                .fullName("Learner").status(status).roles(userRoles).build();
    }
}
