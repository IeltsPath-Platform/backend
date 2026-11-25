package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.PasswordRehashMetrics;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordEncoder legacyPasswordEncoder;
    @Mock private AuthTokenIssuer authTokenIssuer;
    @Mock private PasswordRehashMetrics rehashMetrics;

    @BeforeEach
    void configureDummyHash() {
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        when(legacyPasswordEncoder.encode(anyString())).thenReturn("legacy-dummy-hash");
    }

    @Test
    void issuesTokensForActiveUserWithValidPassword() {
        LoginUseCase useCase = useCase();
        User user = userWithStatus(UserStatus.ACTIVE, "hash");
        AuthTokenResult tokens = new AuthTokenResult("access", "refresh", 3600, 604800);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(authTokenIssuer.issue(user)).thenReturn(tokens);
        assertThat(useCase.execute("John@Example.com", "secret123")).isEqualTo(tokens);
        verify(passwordEncoder).matches("secret123", "hash");
        verify(authTokenIssuer).issue(user);
    }

    @Test
    void rejectsInvalidPasswordAfterComparingStoredHashOnce() {
        LoginUseCase useCase = useCase();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(userWithStatus(UserStatus.ACTIVE, "hash")));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        when(passwordEncoder.upgradeEncoding("hash")).thenReturn(false);
        assertThatThrownBy(() -> useCase.execute("john@example.com", "bad"))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Email hoặc mật khẩu không đúng");
        verify(passwordEncoder).matches("bad", "hash");
        verify(legacyPasswordEncoder).matches("bad", "legacy-dummy-hash");
    }

    @Test
    void checksADummyHashWhenEmailHasNoAccount() {
        LoginUseCase useCase = useCase();
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("secret123", "dummy-hash")).thenReturn(false);
        when(legacyPasswordEncoder.matches("secret123", "legacy-dummy-hash")).thenReturn(false);
        assertThatThrownBy(() -> useCase.execute("nobody@example.com", "secret123"))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Email hoặc mật khẩu không đúng");
        verify(passwordEncoder).matches("secret123", "dummy-hash");
        verify(legacyPasswordEncoder).matches("secret123", "legacy-dummy-hash");
        verify(authTokenIssuer, never()).issue(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void checksADummyHashForEveryInactiveAccountStatus() {
        LoginUseCase useCase = useCase();
        for (UserStatus status : new UserStatus[]{UserStatus.LOCKED, UserStatus.INACTIVE, UserStatus.PENDING_VERIFY}) {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(userWithStatus(status, "hash")));
            when(passwordEncoder.matches("secret123", "dummy-hash")).thenReturn(false);
            when(legacyPasswordEncoder.matches("secret123", "legacy-dummy-hash")).thenReturn(false);
            assertThatThrownBy(() -> useCase.execute("john@example.com", "secret123"))
                    .isInstanceOf(AuthenticationFailedException.class).hasMessage("Email hoặc mật khẩu không đúng");
        }
        verify(passwordEncoder, times(3)).matches("secret123", "dummy-hash");
        verify(legacyPasswordEncoder, times(3)).matches("secret123", "legacy-dummy-hash");
        verify(authTokenIssuer, never()).issue(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void checksADummyHashWhenAccountHasNoPassword() {
        LoginUseCase useCase = useCase();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(userWithStatus(UserStatus.ACTIVE, null)));
        when(passwordEncoder.matches("secret123", "dummy-hash")).thenReturn(false);
        when(legacyPasswordEncoder.matches("secret123", "legacy-dummy-hash")).thenReturn(false);
        assertThatThrownBy(() -> useCase.execute("john@example.com", "secret123")).isInstanceOf(AuthenticationFailedException.class);
        verify(passwordEncoder).matches("secret123", "dummy-hash");
        verify(legacyPasswordEncoder).matches("secret123", "legacy-dummy-hash");
    }

    @Test
    void checksADummyHashForMalformedOrMissingEmail() {
        LoginUseCase useCase = useCase();
        when(passwordEncoder.matches("secret123", "dummy-hash")).thenReturn(false);
        when(legacyPasswordEncoder.matches("secret123", "legacy-dummy-hash")).thenReturn(false);
        assertThatThrownBy(() -> useCase.execute("bad email", "secret123")).isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> useCase.execute(null, "secret123")).isInstanceOf(AuthenticationFailedException.class);
        verify(passwordEncoder, times(2)).matches("secret123", "dummy-hash");
    }

    @Test
    void checksADummyHashForMissingPassword() {
        LoginUseCase useCase = useCase();
        when(passwordEncoder.matches("", "dummy-hash")).thenReturn(false);
        when(legacyPasswordEncoder.matches("", "legacy-dummy-hash")).thenReturn(false);
        assertThatThrownBy(() -> useCase.execute("john@example.com", null)).isInstanceOf(AuthenticationFailedException.class);
        verify(passwordEncoder).matches("", "dummy-hash");
        verify(legacyPasswordEncoder).matches("", "legacy-dummy-hash");
    }

    @Test
    void upgradesAnOldPasswordHashAfterAValidPasswordMatch() {
        LoginUseCase useCase = useCase();
        User user = userWithStatus(UserStatus.ACTIVE, "old-hash");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "old-hash")).thenReturn(true);
        when(passwordEncoder.upgradeEncoding("old-hash")).thenReturn(true);
        when(passwordEncoder.encode("secret123")).thenReturn("new-hash");
        when(userRepository.updatePasswordHashIfUnchanged(user.getId(), "old-hash", "new-hash")).thenReturn(1);
        useCase.execute("john@example.com", "secret123");
        verify(rehashMetrics).recordUpgrade();
    }

    @Test
    void skipsHashUpgradeForInvalidPassword() {
        LoginUseCase useCase = useCase();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(userWithStatus(UserStatus.ACTIVE, "current")));
        when(passwordEncoder.matches("wrong", "current")).thenReturn(false);
        assertThatThrownBy(() -> useCase.execute("john@example.com", "wrong")).isInstanceOf(AuthenticationFailedException.class);
        verify(passwordEncoder, never()).encode("wrong");
        verify(userRepository, never()).updatePasswordHashIfUnchanged(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(rehashMetrics, never()).recordUpgrade();
    }

    private LoginUseCase useCase() {
        return new LoginUseCase(userRepository, passwordEncoder, legacyPasswordEncoder, authTokenIssuer, rehashMetrics);
    }

    private User userWithStatus(UserStatus status, String passwordHash) {
        return User.builder().id(UUID.randomUUID()).email(new Email("john@example.com"))
                .passwordHash(passwordHash).fullName("John Doe").status(status).build();
    }
}
