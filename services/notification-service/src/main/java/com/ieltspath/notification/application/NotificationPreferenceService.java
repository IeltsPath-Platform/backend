package com.ieltspath.notification.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationPreferenceService {
    private final JdbcTemplate jdbc;

    public NotificationPreferenceService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<NotificationPreference> list(UUID recipientId) {
        var saved = new HashMap<String, Boolean>();
        jdbc.query("SELECT event_type,enabled FROM notification_preferences WHERE recipient_id=?", rs -> {
            saved.put(rs.getString("event_type"), rs.getBoolean("enabled"));
        }, recipientId);
        return NotificationEventTypes.ORDERED.stream()
                .map(type -> new NotificationPreference(type, saved.getOrDefault(type, true))).toList();
    }

    @Transactional
    public NotificationPreference update(UUID recipientId, String eventType, boolean enabled) {
        requireEventType(eventType);
        jdbc.update("""
                INSERT INTO notification_preferences(recipient_id,event_type,enabled,updated_at)
                VALUES (?,?,?,CURRENT_TIMESTAMP)
                ON CONFLICT(recipient_id,event_type) DO UPDATE
                SET enabled=EXCLUDED.enabled, updated_at=EXCLUDED.updated_at
                """, recipientId, eventType, enabled);
        return new NotificationPreference(eventType, enabled);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID recipientId, String eventType) {
        requireEventType(eventType);
        var rows = jdbc.query("SELECT enabled FROM notification_preferences WHERE recipient_id=? AND event_type=?",
                (rs, row) -> rs.getBoolean("enabled"), recipientId, eventType);
        return rows.isEmpty() || rows.getFirst();
    }

    private void requireEventType(String eventType) {
        if (!NotificationEventTypes.ALL.contains(eventType))
            throw new IllegalArgumentException("Unsupported notification event type");
    }
}
