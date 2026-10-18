package com.ieltspath.notification.infrastructure.persistence;

import com.ieltspath.notification.domain.repository.NotificationPreferenceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationPreferenceRepositoryAdapter implements NotificationPreferenceRepository {
    private final JdbcTemplate jdbc;

    public NotificationPreferenceRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Map<String, Boolean> findAll(UUID recipientId) {
        var saved = new HashMap<String, Boolean>();
        jdbc.query("SELECT event_type,enabled FROM notification_preferences WHERE recipient_id=?",
                rs -> { saved.put(rs.getString("event_type"), rs.getBoolean("enabled")); }, recipientId);
        return saved;
    }

    @Override
    public Optional<Boolean> find(UUID recipientId, String eventType) {
        return jdbc.query("SELECT enabled FROM notification_preferences WHERE recipient_id=? AND event_type=?",
                (rs, row) -> rs.getBoolean("enabled"), recipientId, eventType).stream().findFirst();
    }

    @Override
    public void save(UUID recipientId, String eventType, boolean enabled) {
        jdbc.update("""
                INSERT INTO notification_preferences(recipient_id,event_type,enabled,updated_at)
                VALUES (?,?,?,CURRENT_TIMESTAMP)
                ON CONFLICT(recipient_id,event_type) DO UPDATE
                SET enabled=EXCLUDED.enabled, updated_at=EXCLUDED.updated_at
                """, recipientId, eventType, enabled);
    }
}
