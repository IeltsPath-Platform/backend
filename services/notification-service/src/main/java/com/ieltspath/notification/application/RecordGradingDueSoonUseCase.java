package com.ieltspath.notification.application;

import com.ieltspath.events.assessment.GradingDueSoonV1;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Turns "a job you were chosen for will be late soon" into a stored notification for the examiner. Delivery is
 * at-least-once, so the source event id makes the insert idempotent. The reminder asks the examiner to act, so it is not
 * subject to the community notification preferences. Grading is blind and the event has no learner, so the examiner is
 * also recorded as the actor.
 */
@Service
public class RecordGradingDueSoonUseCase {
    private final NotificationRepository notifications;

    public RecordGradingDueSoonUseCase(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /**
     * @param routingKey the routing key the message arrived with; must equal the event type
     * @param messageId  the broker message id; must equal the event id
     * @return the stored notification, or empty when it was a duplicate
     * @throws IllegalArgumentException when the event breaks the published contract
     */
    @Transactional
    public Optional<Notification> execute(GradingDueSoonV1 event, String routingKey, String messageId) {
        if (event == null || event.eventId() == null || event.examinerId() == null || event.gradingJobId() == null
                || event.occurredAt() == null || !GradingDueSoonV1.EVENT_TYPE.equals(event.eventType())
                || !event.eventType().equals(routingKey) || !event.eventId().toString().equals(messageId)) {
            throw new IllegalArgumentException("Unsupported grading due-soon event");
        }
        var notification = new Notification(event.eventId(), event.examinerId(), event.examinerId(), event.eventType(),
                null, null, event.gradingJobId(), event.occurredAt(), null);
        return notifications.insertIfAbsent(event.eventId(), notification) ? Optional.of(notification) : Optional.empty();
    }
}
