package com.ieltspath.notification.application;

import com.ieltspath.events.assessment.AssessmentCompletedV2;
import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class RecordAssessmentResultReadyUseCase {
    private static final Set<String> RESULT_TYPES = Set.of("PLACEMENT", "MOCK");
    private final NotificationRepository notifications;
    private final NotificationPreferenceService preferences;
    private final EnqueueNotificationPushUseCase enqueuePush;

    public RecordAssessmentResultReadyUseCase(NotificationRepository notifications,
                                              NotificationPreferenceService preferences,
                                              EnqueueNotificationPushUseCase enqueuePush) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.enqueuePush = enqueuePush;
    }

    @Transactional
    public Optional<Notification> execute(AssessmentCompletedV2 event, String routingKey, String messageId) {
        if (event == null || event.eventId() == null || event.occurredAt() == null || event.data() == null
                || event.data().userId() == null || event.data().attemptId() == null || event.data().resultId() == null
                || event.data().completedAt() == null || event.data().resultVersion() < 1
                || !AssessmentCompletedV2.SOURCE.equals(event.source())
                || !AssessmentCompletedV2.EVENT_TYPE.equals(event.eventType())
                || !AssessmentMessaging.COMPLETED_ROUTING_KEY.equals(routingKey)
                || !event.eventId().toString().equals(messageId)) {
            throw new IllegalArgumentException("Unsupported assessment completed event");
        }
        var data = event.data();
        if (!"COMPLETED".equals(data.status()) || !RESULT_TYPES.contains(data.assessmentType()) || data.resultVersion() != 1) {
            return Optional.empty();
        }
        if (!preferences.isEnabled(data.userId(), NotificationEventTypes.RESULT_READY)) return Optional.empty();
        var notice = new Notification(event.eventId(), data.userId(), data.userId(), NotificationEventTypes.RESULT_READY,
                null, null, null, event.occurredAt(), null);
        if (!notifications.insertIfAbsent(event.eventId(), notice)) return Optional.empty();
        enqueuePush.execute(notice);
        return Optional.of(notice);
    }
}
