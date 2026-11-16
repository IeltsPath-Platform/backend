package com.ieltspath.notification.application;

import com.ieltspath.events.library.LibraryMessaging;
import com.ieltspath.events.library.VocabularyReviewDueV1;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Stores the "words are due for review" notice for the learner who has them. The event id is the notice id, so a redelivered
 * event stores nothing a second time. A learner who switched this notice off gets nothing.
 */
@Service
public class RecordVocabularyReviewDueUseCase {
    private final NotificationRepository notifications;
    private final NotificationPreferenceService preferences;
    private final EnqueueNotificationPushUseCase enqueuePush;

    public RecordVocabularyReviewDueUseCase(NotificationRepository notifications,
                                            NotificationPreferenceService preferences,
                                            EnqueueNotificationPushUseCase enqueuePush) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.enqueuePush = enqueuePush;
    }

    /** @return the stored notice, or empty when it was skipped or already stored. */
    @Transactional
    public Optional<Notification> execute(VocabularyReviewDueV1 event, String routingKey, String messageId) {
        if (event == null || event.eventId() == null || event.occurredAt() == null || event.userId() == null
                || event.reviewDate() == null || event.dueCount() < 1
                || !VocabularyReviewDueV1.EVENT_TYPE.equals(event.eventType())
                || !LibraryMessaging.VOCABULARY_REVIEW_DUE_ROUTING_KEY.equals(routingKey)
                || !event.eventId().toString().equals(messageId)) {
            throw new IllegalArgumentException("Unsupported vocabulary review due event");
        }
        if (!preferences.isEnabled(event.userId(), NotificationEventTypes.VOCABULARY_REVIEW_DUE)) return Optional.empty();
        var notice = new Notification(event.eventId(), event.userId(), event.userId(),
                NotificationEventTypes.VOCABULARY_REVIEW_DUE, null, null, null, event.occurredAt(), null);
        if (!notifications.insertIfAbsent(event.eventId(), notice)) return Optional.empty();
        enqueuePush.execute(notice);
        return Optional.of(notice);
    }
}
