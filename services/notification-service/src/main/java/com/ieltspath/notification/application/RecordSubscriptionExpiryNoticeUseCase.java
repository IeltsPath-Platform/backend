package com.ieltspath.notification.application;

import com.ieltspath.events.access.SubscriptionExpiryNoticeV1;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class RecordSubscriptionExpiryNoticeUseCase {
    private static final Set<String> STAGES = Set.of("SEVEN_DAYS", "ONE_DAY", "EXPIRED");

    private final NotificationRepository notifications;
    private final NotificationPreferenceService preferences;
    private final EnqueueNotificationPushUseCase enqueuePush;

    public RecordSubscriptionExpiryNoticeUseCase(NotificationRepository notifications,
                                                 NotificationPreferenceService preferences,
                                                 EnqueueNotificationPushUseCase enqueuePush) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.enqueuePush = enqueuePush;
    }

    @Transactional
    public Optional<Notification> execute(SubscriptionExpiryNoticeV1 event, String routingKey, String messageId) {
        if (event == null || event.eventId() == null || event.userId() == null || event.subscriptionId() == null
                || event.occurredAt() == null || event.endsAt() == null || !STAGES.contains(event.stage())
                || !SubscriptionExpiryNoticeV1.EVENT_TYPE.equals(event.eventType())
                || !event.eventType().equals(routingKey) || !event.eventId().toString().equals(messageId)) {
            throw new IllegalArgumentException("Unsupported subscription expiry notice");
        }
        if (!preferences.isEnabled(event.userId(), event.eventType())) return Optional.empty();
        var notice = new Notification(event.eventId(), event.userId(), event.userId(), event.eventType(),
                null, null, null, event.occurredAt(), null);
        if (!notifications.insertIfAbsent(event.eventId(), notice)) return Optional.empty();
        enqueuePush.execute(notice);
        return Optional.of(notice);
    }
}
