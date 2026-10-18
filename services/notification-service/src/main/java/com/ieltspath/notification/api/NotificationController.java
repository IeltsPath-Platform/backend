package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.notification.application.NotificationPreference;
import com.ieltspath.notification.application.NotificationPreferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider users;
    private final NotificationPreferenceService preferences;
    public NotificationController(JdbcTemplate jdbc, CurrentUserProvider users, NotificationPreferenceService preferences) { this.jdbc = jdbc; this.users = users; this.preferences = preferences; }

    @GetMapping
    public List<Item> list(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page or size");
        UUID owner = users.requireCurrentUser().id();
        return jdbc.query("SELECT id,actor_id,event_type,post_id,comment_id,created_at,read_at FROM notifications WHERE recipient_id=? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",
                (rs, n) -> new Item(rs.getObject("id", UUID.class), rs.getObject("actor_id", UUID.class), rs.getString("event_type"), rs.getObject("post_id", UUID.class), rs.getObject("comment_id", UUID.class), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant()), owner, size, (long) page * size);
    }
    @GetMapping("/unread-count")
    public Count unreadCount() { return new Count(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=? AND read_at IS NULL", Long.class, users.requireCurrentUser().id())); }
    @PutMapping("/{id}/read")
    public void markRead(@PathVariable("id") UUID id) {
        int updated = jdbc.update("UPDATE notifications SET read_at=COALESCE(read_at,?) WHERE id=? AND recipient_id=?",
                java.sql.Timestamp.from(Instant.now()), id, users.requireCurrentUser().id());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
    }
    @PutMapping("/read-all")
    public Count markAllRead() { int count = jdbc.update("UPDATE notifications SET read_at=? WHERE recipient_id=? AND read_at IS NULL", java.sql.Timestamp.from(Instant.now()), users.requireCurrentUser().id()); return new Count(count); }
    @GetMapping("/preferences")
    public List<NotificationPreference> preferences() { return preferences.list(users.requireCurrentUser().id()); }
    @PutMapping("/preferences/{eventType}")
    public NotificationPreference updatePreference(@PathVariable("eventType") String eventType, @Valid @RequestBody PreferenceRequest request) {
        try { return preferences.update(users.requireCurrentUser().id(), eventType, request.enabled()); }
        catch (IllegalArgumentException invalid) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, invalid.getMessage()); }
    }
    public record PreferenceRequest(@NotNull Boolean enabled) { }
    public record Item(UUID id, UUID actorId, String eventType, UUID postId, UUID commentId, Instant createdAt, Instant readAt) {}
    public record Count(long count) {}
}
