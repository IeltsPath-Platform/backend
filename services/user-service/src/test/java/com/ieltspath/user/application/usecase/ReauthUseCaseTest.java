package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.exception.ReauthAccountMismatchException;
import com.ieltspath.user.application.exception.ReauthRequiredException;
import com.ieltspath.user.application.port.GoogleIdTokenVerifier;
import com.ieltspath.user.application.port.GoogleNonceStore;
import com.ieltspath.user.application.port.ReauthTokenStore;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.result.GoogleIdentity;
import com.ieltspath.user.config.GoogleAuthProperties;
import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.aggregate.PasswordChangeAttempts;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.PasswordChangeAttemptRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReauthUseCaseTest {
    @Mock UserRepository users;
    @Mock OAuthIdentityRepository identities;
    @Mock PasswordEncoder encoder;
    @Mock UserLock lock;
    @Mock PasswordChangeAttemptRepository attempts;
    @Mock GoogleIdTokenVerifier verifier;
    @Mock GoogleNonceStore nonces;
    @Mock ReauthTokenStore tokens;
    private ReauthUseCase useCase;
    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final User user = User.builder().id(userId).email(new Email("person@example.test"))
            .fullName("Person").passwordHash("hash").status(UserStatus.ACTIVE)
            .emailVerifiedAt(java.time.LocalDateTime.now()).build();

    @BeforeEach
    void setUp() {
        useCase = new ReauthUseCase(users, identities, encoder, lock, attempts, verifier, nonces,
                new GoogleAuthProperties(), tokens);
        when(users.findById(userId)).thenReturn(Optional.of(user));
    }

    @Test
    void verifiesThePasswordAndIssuesASessionBoundOneTimeToken() {
        when(encoder.matches("password", "hash")).thenReturn(true);
        when(tokens.issue(userId, "PASSWORD", sessionId)).thenReturn("reauth-token");

        var result = useCase.execute(userId, sessionId, "password", null);

        assertThat(result.token()).isEqualTo("reauth-token");
        assertThat(result.method()).isEqualTo("PASSWORD");
        verify(attempts).deleteByUserId(userId);
    }

    @Test
    void rejectsAnIncorrectPasswordAndPersistsTheSharedFailureCounter() {
        when(encoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(userId, sessionId, "wrong", null))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessage("Mật khẩu hiện tại không chính xác");
        verify(attempts).save(any());
        verify(tokens, never()).issue(any(), any(), any());
    }

    @Test
    void blocksPasswordReauthenticationWhileTheSharedFailureLockIsActive() {
        var now = java.time.LocalDateTime.now();
        when(attempts.findByUserId(userId)).thenReturn(Optional.of(
                new PasswordChangeAttempts(userId, 5, now.minusMinutes(2), now.plusMinutes(10))));

        assertThatThrownBy(() -> useCase.execute(userId, sessionId, "password", null))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessage("Đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau 15 phút.");
    }

    @Test
    void rejectsMissingCredentialsOrMissingSessionClaims() {
        assertThatThrownBy(() -> useCase.execute(userId, sessionId, null, null))
                .isInstanceOf(ReauthRequiredException.class);
        assertThatThrownBy(() -> useCase.execute(userId, null, "password", null))
                .isInstanceOf(ReauthRequiredException.class);
    }

    @Test
    void verifiesTheLinkedGoogleSubjectAndConsumesItsNonce() {
        var identity = OAuthIdentity.builder().userId(userId).provider(OAuthProvider.GOOGLE)
                .providerSubject("linked-sub").build();
        when(verifier.verify("google-token")).thenReturn(new GoogleIdentity("linked-sub", "person@gmail.com", "Person", null, "nonce"));
        when(nonces.consume("nonce")).thenReturn(true);
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE)).thenReturn(Optional.of(identity));
        when(tokens.issue(userId, "GOOGLE", sessionId)).thenReturn("reauth-token");

        var result = useCase.execute(userId, sessionId, null, "google-token");

        assertThat(result.method()).isEqualTo("GOOGLE");
        verify(nonces).consume("nonce");
    }

    @Test
    void rejectsAnUnlinkedOrDifferentGoogleAccount() {
        when(verifier.verify("google-token")).thenReturn(new GoogleIdentity("other-sub", "person@gmail.com", "Person", null));
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(userId, sessionId, null, "google-token"))
                .isInstanceOf(ReauthAccountMismatchException.class)
                .hasMessage("Tài khoản Google không khớp với tài khoản Google đã liên kết.");
        verify(tokens, never()).issue(eq(userId), eq("GOOGLE"), eq(sessionId));
    }
}
