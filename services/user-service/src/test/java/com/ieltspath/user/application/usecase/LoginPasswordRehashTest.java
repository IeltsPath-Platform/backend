package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.PasswordRehashMetrics;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginPasswordRehashTest {
    @Test
    void legacyPasswordAuthenticatesAfterUpgradeAndOnTheNextLogin() {
        BCryptPasswordEncoder currentEncoder = new BCryptPasswordEncoder(12);
        BCryptPasswordEncoder legacyEncoder = new BCryptPasswordEncoder(10);
        UserRepository users = mock(UserRepository.class);
        AuthTokenIssuer issuer = mock(AuthTokenIssuer.class);
        PasswordRehashMetrics metrics = mock(PasswordRehashMetrics.class);
        User user = User.builder().id(UUID.randomUUID()).email(new Email("john@example.com"))
                .fullName("John Doe").status(UserStatus.ACTIVE)
                .passwordHash(legacyEncoder.encode("Strong password 2026!")).build();
        when(users.findByEmail("john@example.com")).thenAnswer(invocation -> Optional.of(user));
        when(users.updatePasswordHashIfUnchanged(org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    user.changePassword(invocation.getArgument(2));
                    return 1;
                });
        when(issuer.issue(user)).thenReturn(new AuthTokenResult("access", "refresh", 3600, 604800));
        LoginUseCase login = new LoginUseCase(users, currentEncoder, legacyEncoder, issuer, metrics);

        login.execute("john@example.com", "Strong password 2026!");

        assertThat(user.getPasswordHash()).startsWith("$2a$12$");
        assertThat(currentEncoder.matches("Strong password 2026!", user.getPasswordHash())).isTrue();

        assertThat(login.execute("john@example.com", "Strong password 2026!").accessToken()).isEqualTo("access");
    }
}
