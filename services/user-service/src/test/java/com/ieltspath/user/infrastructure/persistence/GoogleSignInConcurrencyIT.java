package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.port.GoogleIdTokenVerifier;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.application.result.GoogleIdentity;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.usecase.AuthTokenIssuer;
import com.ieltspath.user.application.usecase.GoogleSignInUseCase;
import com.ieltspath.user.application.usecase.LinkGoogleAccountUseCase;
import com.ieltspath.user.application.usecase.LoginHistoryRecorder;
import com.ieltspath.user.application.usecase.RefreshTokenUseCase;
import com.ieltspath.user.application.usecase.TokenHashService;
import com.ieltspath.user.domain.aggregate.RefreshToken;
import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.RoleRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import com.ieltspath.user.infrastructure.adapter.AdvisoryUserLock;
import com.ieltspath.user.infrastructure.adapter.OAuthIdentityRepositoryAdapter;
import com.ieltspath.user.infrastructure.adapter.RefreshTokenRepositoryAdapter;
import com.ieltspath.user.infrastructure.adapter.RoleRepositoryAdapter;
import com.ieltspath.user.infrastructure.adapter.UserRepositoryAdapter;
import com.ieltspath.user.infrastructure.persistence.mapper.OAuthIdentityMapperImpl;
import com.ieltspath.user.infrastructure.persistence.mapper.RoleMapperImpl;
import com.ieltspath.user.infrastructure.persistence.mapper.UserMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GoogleSignInUseCase.class, LinkGoogleAccountUseCase.class, RefreshTokenUseCase.class,
        UserRepositoryAdapter.class,
        OAuthIdentityRepositoryAdapter.class, RoleRepositoryAdapter.class, RefreshTokenRepositoryAdapter.class,
        AdvisoryUserLock.class, TokenHashService.class, UserMapperImpl.class, RoleMapperImpl.class,
        OAuthIdentityMapperImpl.class, GoogleSignInConcurrencyIT.Fakes.class})
class GoogleSignInConcurrencyIT {
    private static final GoogleIdentity GOOGLE = new GoogleIdentity(
            "google-concurrency-subject", "concurrent@gmail.com", "Concurrent Learner", null);
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class Fakes {
        @Bean GoogleIdTokenVerifier verifier() {
            return token -> "existing-token".equals(token)
                    ? new GoogleIdentity("existing-google-subject", "existing@gmail.com", "Existing Learner", null)
                    : GOOGLE;
        }
        @Bean LoginHistoryRecorder loginHistoryRecorder() { return mock(LoginHistoryRecorder.class); }
        @Bean AuthTokenIssuer authTokenIssuer(RefreshTokenRepository refreshTokens, TokenHashService tokenHashService) {
            AuthTokenIssuer issuer = mock(AuthTokenIssuer.class);
            when(issuer.issue(any(User.class))).thenAnswer(call -> {
                User user = call.getArgument(0);
                String refreshToken = "refresh-" + UUID.randomUUID();
                refreshTokens.save(RefreshToken.builder().userId(user.getId())
                        .tokenHash(tokenHashService.hash(refreshToken))
                        .expiresAt(LocalDateTime.now().plusDays(7)).build());
                return new AuthTokenResult("access-" + UUID.randomUUID(), refreshToken, 3600, 604800);
            });
            return issuer;
        }
    }

    @Autowired GoogleSignInUseCase signIn;
    @Autowired RefreshTokenUseCase refreshTokenUseCase;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired TokenHashService tokenHashService;
    @Autowired UserLock userLock;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void simultaneousFirstSignInsCreateOneUserAndOneIdentity() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            var first = pool.submit(() -> signIn("first", ready, start));
            var second = pool.submit(() -> signIn("second", ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            var firstResult = first.get(30, TimeUnit.SECONDS);
            var secondResult = second.get(30, TimeUnit.SECONDS);
            assertThat(firstResult.userId()).isEqualTo(secondResult.userId());
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class,
                    "concurrent@gmail.com")).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM oauth_identities WHERE provider = 'GOOGLE' AND provider_subject = ?",
                    Integer.class, GOOGLE.subject())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class,
                    "concurrent@gmail.com")).isNull();
        }
    }

    @Test
    void concurrentRefreshAndGoogleLinkLeaveOnlyTheNewGoogleSessionActive() throws Exception {
        Set<Role> customerRole = new HashSet<>(roles.findByNames(Set.of(RoleName.CUSTOMER.name())));
        User passwordUser = users.save(User.builder().email(new Email("existing@gmail.com"))
                .passwordHash("existing-hash").fullName("Existing Learner").status(UserStatus.ACTIVE)
                .roles(customerRole).build());
        UUID userId = passwordUser.getId();
        String oldRawToken = "old-refresh-token";
        refreshTokens.save(RefreshToken.builder().userId(userId).tokenHash(tokenHashService.hash(oldRawToken))
                .expiresAt(LocalDateTime.now().plusDays(7)).build());

        try (var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            TransactionTemplate holdUserLock = new TransactionTemplate(transactionManager);
            @SuppressWarnings("unchecked")
            var tasks = holdUserLock.execute(status -> {
                userLock.lock(userId);
                var refresh = pool.submit(() -> runConcurrent(ready, start,
                        () -> refreshTokenUseCase.execute(oldRawToken)));
                var google = pool.submit(() -> runConcurrent(ready, start,
                        () -> signIn.execute("existing-token", LoginContext.UNKNOWN)));
                try {
                    if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Sign-in barrier timed out");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return new java.util.AbstractMap.SimpleImmutableEntry<>(refresh, google);
            });
            start.countDown();
            var refreshFuture = tasks.getKey();
            var googleResult = tasks.getValue().get(30, TimeUnit.SECONDS);
            try {
                refreshFuture.get(30, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException expectedWhenGoogleLocksFirst) {
                assertThat(expectedWhenGoogleLocksFirst.getCause()).isInstanceOf(AuthenticationFailedException.class);
            }

            assertThat(googleResult.userId()).isEqualTo(userId);
            String googleTokenHash = tokenHashService.hash(googleResult.tokens().refreshToken());
            assertThat(jdbc.queryForObject("SELECT revoked_at IS NULL FROM refresh_tokens WHERE token_hash = ?",
                    Boolean.class, googleTokenHash)).isTrue();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
                    Integer.class, userId)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT revoked_at IS NULL FROM refresh_tokens WHERE token_hash = ?",
                    Boolean.class, tokenHashService.hash(oldRawToken))).isFalse();
            assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, userId)).isNull();
        }
    }

    private com.ieltspath.user.application.result.GoogleSignInResult signIn(
            String request, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Sign-in barrier timed out");
        return signIn.execute(request, LoginContext.UNKNOWN);
    }

    private <T> T runConcurrent(CountDownLatch ready, CountDownLatch start,
                                java.util.concurrent.Callable<T> work) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrency barrier timed out");
        return work.call();
    }
}
