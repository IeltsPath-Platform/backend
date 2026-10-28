package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.application.command.ChangePasswordCommand;
import com.ieltspath.user.application.usecase.ChangePasswordUseCase;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import com.ieltspath.user.infrastructure.adapter.AdvisoryUserLock;
import com.ieltspath.user.infrastructure.adapter.PasswordChangeAttemptRepositoryAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The wrong-password counter lives in PostgreSQL: it survives the exception that reports the failure and it is seen
 * by every service instance. The use case runs in its own real transactions here, so the test itself is not one.
 */
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ChangePasswordUseCase.class, PasswordChangeAttemptRepositoryAdapter.class, AdvisoryUserLock.class,
        PasswordChangeAttemptsPersistenceTest.Fakes.class})
class PasswordChangeAttemptsPersistenceTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final UUID USER_ID = UUID.randomUUID();

    @TestConfiguration
    static class Fakes {
        @Bean UserRepository users() {
            UserRepository users = mock(UserRepository.class);
            when(users.findById(USER_ID)).thenAnswer(inv -> Optional.of(User.builder().id(USER_ID)
                    .email(new Email("person@example.com")).fullName("Person").status(UserStatus.ACTIVE)
                    .passwordHash(ENCODER.encode("right-password")).build()));
            return users;
        }
        @Bean RefreshTokenRepository refreshTokens() { return mock(RefreshTokenRepository.class); }
        @Bean PasswordEncoder passwordEncoder() { return ENCODER; }
    }

    @Autowired ChangePasswordUseCase useCase;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void insertUser() {
        jdbc.update("INSERT INTO users (id, email, full_name, password_hash, status) VALUES (?, 'person@example.test', 'Person', 'hash', 'ACTIVE')", USER_ID);
    }

    @AfterEach
    void removeUser() {
        jdbc.update("DELETE FROM users WHERE id = ?", USER_ID);
    }

    private int storedFailures() {
        return jdbc.queryForObject("SELECT COALESCE(MAX(failures), 0) FROM password_change_attempts WHERE user_id = ?", Integer.class, USER_ID);
    }

    @Test
    void aWrongPasswordIsCountedEvenThoughItEndsInAnException() {
        var error = assertThrows(PasswordChangeException.class,
                () -> useCase.execute(new ChangePasswordCommand(USER_ID, "wrong", "next-password")));

        assertEquals("CURRENT_PASSWORD_INVALID", error.getCode());
        assertEquals(1, storedFailures());
    }

    @Test
    void fiveFailuresLockEvenTheRightPasswordAndASecondInstanceSeesTheSameCount() {
        for (int i = 0; i < 5; i++) {
            assertThrows(PasswordChangeException.class,
                    () -> useCase.execute(new ChangePasswordCommand(USER_ID, "wrong", "next-password")));
        }
        assertEquals(5, storedFailures());

        // Another instance has no memory of its own: only the database can tell it the user is locked.
        ChangePasswordUseCase otherInstance = new ChangePasswordUseCase(
                mock(UserRepository.class), mock(RefreshTokenRepository.class), ENCODER,
                userId -> { }, new PasswordChangeAttemptRepositoryAdapter(jdbc));
        var limited = assertThrows(PasswordChangeException.class,
                () -> otherInstance.execute(new ChangePasswordCommand(USER_ID, "right-password", "next-password")));

        assertEquals("PASSWORD_ATTEMPTS_LIMITED", limited.getCode());
        assertEquals(5, storedFailures());
    }

    @Test
    void aSuccessfulChangeClearsTheFailures() {
        assertThrows(PasswordChangeException.class,
                () -> useCase.execute(new ChangePasswordCommand(USER_ID, "wrong", "next-password")));

        useCase.execute(new ChangePasswordCommand(USER_ID, "right-password", "next-password"));

        assertEquals(0, storedFailures());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM password_change_attempts WHERE user_id = ?", Integer.class, USER_ID));
    }

    @Test
    void removingTheUserRemovesItsAttempts() {
        assertThrows(PasswordChangeException.class,
                () -> useCase.execute(new ChangePasswordCommand(USER_ID, "wrong", "next-password")));

        jdbc.update("DELETE FROM users WHERE id = ?", USER_ID);

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM password_change_attempts WHERE user_id = ?", Integer.class, USER_ID));
    }
}
