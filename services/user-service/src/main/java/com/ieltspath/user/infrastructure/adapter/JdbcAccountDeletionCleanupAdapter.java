package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.AccountDeletionCleanupPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcAccountDeletionCleanupAdapter implements AccountDeletionCleanupPort {
    private final JdbcTemplate jdbc;

    public JdbcAccountDeletionCleanupAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> anonymizeProfileAndDeleteIdentityData(UUID userId) {
        // Mail events (reset and verification tokens, email, name) must not be sent or kept; the deletion event
        // itself is written after this call and is kept.
        jdbc.update("DELETE FROM outbox_events WHERE aggregate_id = ? AND event_type <> ?",
                userId, com.ieltspath.events.user.UserDeletedV1.EVENT_TYPE);
        String oldAvatar = jdbc.query(
                "SELECT avatar_reference FROM learner_profiles WHERE user_id = ?",
                rs -> rs.next() ? rs.getString("avatar_reference") : null,
                userId);
        jdbc.update("""
                UPDATE learner_profiles
                SET display_name = 'Người dùng đã xóa',
                    avatar_reference = NULL,
                    bio = NULL,
                    self_reported_band = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """, userId);
        jdbc.update("DELETE FROM oauth_identities WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM account_action_tokens WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM password_change_attempts WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM login_history WHERE user_id = ?", userId);
        // An examiner's band and certificates are personal; the row stays for the staff screens but says nothing.
        jdbc.update("""
                UPDATE examiner_profiles
                SET ielts_band = NULL, certificates = '[]'::jsonb, accepting_submissions = FALSE,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """, userId);
        return Optional.ofNullable(oldAvatar);
    }

    @Override
    public void queueAvatarDeletion(UUID userId, String objectKey) {
        jdbc.update("""
                INSERT INTO pending_avatar_deletions (object_key, user_id) VALUES (?, ?)
                ON CONFLICT (object_key) DO NOTHING
                """, objectKey, userId);
    }
}
