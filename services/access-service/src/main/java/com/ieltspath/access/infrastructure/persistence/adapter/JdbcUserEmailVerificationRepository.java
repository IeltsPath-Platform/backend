package com.ieltspath.access.infrastructure.persistence.adapter;

import com.ieltspath.access.domain.repository.UserEmailVerificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcUserEmailVerificationRepository implements UserEmailVerificationRepository {
    private final JdbcTemplate jdbc;

    public JdbcUserEmailVerificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void recordState(UUID userId, boolean verified, String email, String displayName, Instant occurredAt) {
        // An older event arriving late must not overwrite a newer state. A missing contact keeps the one already held.
        jdbc.update("""
                INSERT INTO user_email_verification_state (user_id, verified, email, display_name, occurred_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET
                    verified = EXCLUDED.verified,
                    email = COALESCE(EXCLUDED.email, user_email_verification_state.email),
                    display_name = COALESCE(EXCLUDED.display_name, user_email_verification_state.display_name),
                    occurred_at = EXCLUDED.occurred_at
                WHERE user_email_verification_state.occurred_at < EXCLUDED.occurred_at
                """, userId, verified, email, displayName, Timestamp.from(occurredAt));
    }

    @Override
    public boolean isUnverified(UUID userId) {
        Boolean unverified = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM user_email_verification_state WHERE user_id = ? AND verified = FALSE)
                """, Boolean.class, userId);
        return Boolean.TRUE.equals(unverified);
    }

    @Override
    public Optional<Contact> findContact(UUID userId) {
        return jdbc.query("""
                SELECT email, display_name FROM user_email_verification_state WHERE user_id = ? AND email IS NOT NULL
                """, (rs, row) -> new Contact(rs.getString("email"), rs.getString("display_name")), userId)
                .stream().findFirst();
    }
}
