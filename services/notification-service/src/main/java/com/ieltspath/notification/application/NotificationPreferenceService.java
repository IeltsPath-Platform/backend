package com.ieltspath.notification.application;

import com.ieltspath.events.community.CommunityEventTypes;

import com.ieltspath.notification.domain.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationPreferenceService {
    private final NotificationPreferenceRepository preferences;

    public NotificationPreferenceService(NotificationPreferenceRepository preferences) { this.preferences = preferences; }

    @Transactional(readOnly = true)
    public List<NotificationPreference> list(UUID recipientId) {
        var saved = preferences.findAll(recipientId);
        return CommunityEventTypes.ORDERED.stream()
                .map(type -> new NotificationPreference(type, saved.getOrDefault(type, true))).toList();
    }

    @Transactional
    public NotificationPreference update(UUID recipientId, String eventType, boolean enabled) {
        requireEventType(eventType);
        preferences.save(recipientId, eventType, enabled);
        return new NotificationPreference(eventType, enabled);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID recipientId, String eventType) {
        requireEventType(eventType);
        return preferences.find(recipientId, eventType).orElse(true);
    }

    private void requireEventType(String eventType) {
        if (!CommunityEventTypes.ALL.contains(eventType))
            throw new IllegalArgumentException("Unsupported notification event type");
    }
}
