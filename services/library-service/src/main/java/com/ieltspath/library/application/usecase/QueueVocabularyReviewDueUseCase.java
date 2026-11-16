package com.ieltspath.library.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.library.VocabularyReviewDueV1;
import com.ieltspath.library.application.port.ReviewDueReminders;
import com.ieltspath.library.application.port.ReviewDueReminders.DueLearner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Queues one "words are due for review" event per learner per day. The event id comes from the learner and the day, so a
 * second run on the same day (two instances firing together, or a restart) queues nothing new. The day is the calendar
 * day in the configured zone, the same zone the notebook summary uses by default.
 */
@Service
public class QueueVocabularyReviewDueUseCase {
    static final String AGGREGATE_TYPE = "Learner";
    /** Outbox type; the relay maps it to the routing key. */
    public static final String OUTBOX_EVENT_TYPE = "VocabularyReviewDue";
    private static final int PAGE_SIZE = 500;
    private static final UUID FIRST_USER = new UUID(0L, 0L);

    private final ReviewDueReminders reminders;
    private final ObjectMapper objectMapper;
    private final ZoneId zone;
    private final int minCount;

    public QueueVocabularyReviewDueUseCase(ReviewDueReminders reminders, ObjectMapper objectMapper,
                                           @Value("${library.review-due.zone:Asia/Ho_Chi_Minh}") String zone,
                                           @Value("${library.review-due.min-count:5}") int minCount) {
        if (minCount < 1) throw new IllegalArgumentException("library.review-due.min-count must be at least 1");
        this.reminders = reminders;
        this.objectMapper = objectMapper;
        this.zone = ZoneId.of(zone);
        this.minCount = minCount;
    }

    /** Returns how many reminders were newly queued. */
    @Transactional
    public int execute(Instant now) {
        LocalDate day = now.atZone(zone).toLocalDate();
        int queued = 0;
        UUID after = FIRST_USER;
        List<DueLearner> page;
        do {
            page = reminders.findDueLearners(now, minCount, after, PAGE_SIZE);
            for (DueLearner learner : page) {
                UUID eventId = eventId(learner.userId(), day);
                var event = new VocabularyReviewDueV1(eventId, VocabularyReviewDueV1.EVENT_TYPE, now, learner.userId(),
                        day, learner.dueCount());
                if (reminders.queue(eventId, learner.userId(), json(event), now)) queued++;
            }
            if (!page.isEmpty()) after = page.get(page.size() - 1).userId();
        } while (page.size() == PAGE_SIZE);
        return queued;
    }

    static UUID eventId(UUID userId, LocalDate day) {
        return UUID.nameUUIDFromBytes(("vocabulary-review-due:" + userId + ":" + day).getBytes(StandardCharsets.UTF_8));
    }

    private String json(VocabularyReviewDueV1 event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Could not serialize vocabulary review due event", failure);
        }
    }
}
