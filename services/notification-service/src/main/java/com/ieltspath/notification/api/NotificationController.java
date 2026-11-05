package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.notification.application.NotificationInboxUseCase;
import com.ieltspath.notification.application.NotificationPreference;
import com.ieltspath.notification.application.NotificationPreferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationInboxUseCase inbox;
    private final CurrentUserProvider users;
    private final NotificationPreferenceService preferences;
    public NotificationController(NotificationInboxUseCase inbox, CurrentUserProvider users, NotificationPreferenceService preferences) { this.inbox = inbox; this.users = users; this.preferences = preferences; }

    @GetMapping
    public List<Item> list(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size) {
        return inbox.list(users.requireCurrentUser().id(), page, size).stream()
                .map(n -> new Item(n.id(), n.actorId(), n.eventType(), n.postId(), n.commentId(), n.gradingJobId(), n.createdAt(), n.readAt())).toList();
    }
    @GetMapping("/unread-count")
    public Count unreadCount() { return new Count(inbox.unreadCount(users.requireCurrentUser().id())); }
    @PutMapping("/{id}/read")
    public void markRead(@PathVariable("id") UUID id) { inbox.markRead(users.requireCurrentUser().id(), id); }
    @PutMapping("/read-all")
    public Count markAllRead() { return new Count(inbox.markAllRead(users.requireCurrentUser().id())); }
    @GetMapping("/preferences")
    public List<NotificationPreference> preferences() { return preferences.list(users.requireCurrentUser().id()); }
    @PutMapping("/preferences/{eventType}")
    public NotificationPreference updatePreference(@PathVariable("eventType") String eventType, @Valid @RequestBody PreferenceRequest request) {
        try { return preferences.update(users.requireCurrentUser().id(), eventType, request.enabled()); }
        catch (IllegalArgumentException invalid) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, invalid.getMessage()); }
    }
    public record PreferenceRequest(@NotNull Boolean enabled) { }
    public record Item(UUID id, UUID actorId, String eventType, UUID postId, UUID commentId, UUID gradingJobId, Instant createdAt, Instant readAt) {}
    public record Count(long count) {}
}
