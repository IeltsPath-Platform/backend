package com.ieltspath.notification.application;

import com.ieltspath.events.assessment.GradingCompletedV1;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Turns "the examiner finished grading your essay" into a stored notification for the learner, so they can read the
 * comments and the marked errors. Delivery is at-least-once, so the source event id makes the insert idempotent. It tells
 * the learner something they are waiting for, so it is not subject to the community notification preferences. The learner
 * is recorded as the actor too: the notice does not name the examiner, and the app shows who graded the essay.
 */
@Service
public class RecordGradingCompletedUseCase {
    private final NotificationRepository notifications;

    public RecordGradingCompletedUseCase(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /**
     * @param routingKey the routing key the message arrived with; must equal the event type
     * @param messageId  the broker message id; must equal the event id
     * @return the stored notification, or empty when it was a duplicate
     * @throws IllegalArgumentException when the event breaks the published contract
     */
    @Transactional
    public Optional<Notification> execute(GradingCompletedV1 event, String routingKey, String messageId) {
        if (event == null || event.eventId() == null || event.learnerId() == null || event.gradingJobId() == null
                || event.occurredAt() == null || !GradingCompletedV1.EVENT_TYPE.equals(event.eventType())
                || !event.eventType().equals(routingKey) || !event.eventId().toString().equals(messageId)) {
            throw new IllegalArgumentException("Unsupported grading completed event");
        }
        var notification = new Notification(event.eventId(), event.learnerId(), event.learnerId(), event.eventType(),
                null, null, event.gradingJobId(), event.occurredAt(), null);
        return notifications.insertIfAbsent(event.eventId(), notification) ? Optional.of(notification) : Optional.empty();
    }
}
