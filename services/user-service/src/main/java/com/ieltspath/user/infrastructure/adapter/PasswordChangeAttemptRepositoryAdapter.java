package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.PasswordChangeAttempts;
import com.ieltspath.user.domain.repository.PasswordChangeAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PasswordChangeAttemptRepositoryAdapter implements PasswordChangeAttemptRepository {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<PasswordChangeAttempts> findByUserId(UUID userId) {
        return jdbc.query("""
                        SELECT user_id, failures, window_started_at, locked_until
                        FROM password_change_attempts WHERE user_id = ?
                        """,
                (rs, row) -> new PasswordChangeAttempts(
                        rs.getObject("user_id", UUID.class),
                        rs.getInt("failures"),
                        rs.getObject("window_started_at", LocalDateTime.class),
                        rs.getObject("locked_until", LocalDateTime.class)),
                userId).stream().findFirst();
    }

    @Override
    public void save(PasswordChangeAttempts attempts) {
        jdbc.update("""
                        INSERT INTO password_change_attempts (user_id, failures, window_started_at, locked_until)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (user_id) DO UPDATE
                        SET failures = EXCLUDED.failures,
                            window_started_at = EXCLUDED.window_started_at,
                            locked_until = EXCLUDED.locked_until
                        """,
                attempts.getUserId(), attempts.getFailures(), attempts.getWindowStartedAt(), attempts.getLockedUntil());
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jdbc.update("DELETE FROM password_change_attempts WHERE user_id = ?", userId);
    }
}
