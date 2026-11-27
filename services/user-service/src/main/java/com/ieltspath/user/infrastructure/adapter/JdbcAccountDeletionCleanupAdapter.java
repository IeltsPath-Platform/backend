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
        return Optional.ofNullable(oldAvatar);
    }
}
