package com.ieltspath.notification.application;

import com.ieltspath.events.assessment.GradingOverdueV1;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Turns "the examiner you chose is late" into a stored notification for the learner. Delivery is at-least-once, so the
 * source event id makes the insert idempotent. This notice asks the learner to act, so it is not subject to the
 * community notification preferences.
 */
@Service
public class RecordGradingOverdueUseCase {
    private final NotificationRepository notifications;
    private final EnqueueNotificationPushUseCase enqueuePush;
    private final NotificationPreferenceService preferences;

    @Autowired
    public RecordGradingOverdueUseCase(NotificationRepository notifications, EnqueueNotificationPushUseCase enqueuePush,
                                       NotificationPreferenceService preferences) {
        this.notifications = notifications;
        this.enqueuePush = enqueuePush;
        this.preferences = preferences;
    }

    public RecordGradingOverdueUseCase(NotificationRepository notifications, EnqueueNotificationPushUseCase enqueuePush) {
        this(notifications, enqueuePush, null);
    }

    public RecordGradingOverdueUseCase(NotificationRepository notifications) {
        this(notifications, EnqueueNotificationPushUseCase.disabled());
    }

    /**
     * @param routingKey the routing key the message arrived with; must equal the event type
     * @param messageId  the broker message id; must equal the event id
     * @return the stored notification, or empty when it was a duplicate
     * @throws IllegalArgumentException when the event breaks the published contract
     */
    @Transactional
    public Optional<Notification> execute(GradingOverdueV1 event, String routingKey, String messageId) {
        if (event == null || event.eventId() == null || event.learnerId() == null || event.examinerId() == null
                || event.gradingJobId() == null || event.occurredAt() == null
                || !GradingOverdueV1.EVENT_TYPE.equals(event.eventType()) || !event.eventType().equals(routingKey)
                || !event.eventId().toString().equals(messageId)) {
            throw new IllegalArgumentException("Unsupported grading overdue event");
        }
        if (preferences != null && !preferences.isEnabled(event.learnerId(), event.eventType())) return Optional.empty();
        var notification = new Notification(event.eventId(), event.examinerId(), event.learnerId(), event.eventType(),
                null, null, event.gradingJobId(), event.occurredAt(), null);
        if (!notifications.insertIfAbsent(event.eventId(), notification)) return Optional.empty();
        enqueuePush.execute(notification);
        return Optional.of(notification);
    }
}
