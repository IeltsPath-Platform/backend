package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.DueSoonNoticeStore;
import com.ieltspath.assessment.application.service.ExaminerAssignmentPolicy;
import com.ieltspath.assessment.domain.entity.OutboxEvent;
import com.ieltspath.assessment.domain.repository.OutboxEventRepository;
import com.ieltspath.events.assessment.GradingDueSoonV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Reminds the examiner, once, that a job they were chosen for will be late in 12 hours (36 hours after the choice). Like
 * the late notice to the learner, the reminder is an outbox row written in the same transaction that marks the job as
 * reminded: a crash can neither lose the reminder nor send it twice. A job that is already late is not reminded: the
 * learner has been told instead. One examiner grades a whole attempt, so an attempt is reminded once. Handing the job to
 * another examiner clears the mark and their 36 hours start again.
 */
@Service
public class AnnounceDueSoonGradingUseCase {
    static final String AGGREGATE_TYPE = "GradingJob";

    private final DueSoonNoticeStore store;
    private final OutboxEventRepository outbox;
    private final ObjectMapper json;
    private final int batchSize;

    public AnnounceDueSoonGradingUseCase(DueSoonNoticeStore store, OutboxEventRepository outbox, ObjectMapper json,
                                         @Value("${assessment.examiner.due-soon-notice.batch-size:100}") int batchSize) {
        this.store = store;
        this.outbox = outbox;
        this.json = json;
        this.batchSize = batchSize;
    }

    /** @return the number of reminders recorded in this batch. */
    @Transactional
    public int execute(Instant now) {
        var soon = store.claimUnreminded(now.minus(ExaminerAssignmentPolicy.REMINDER_AFTER),
                now.minus(ExaminerAssignmentPolicy.DEADLINE), batchSize);
        if (soon.isEmpty()) return 0;
        Set<Object> reminded = new HashSet<>();
        Set<UUID> jobIds = new LinkedHashSet<>();
        Set<UUID> attemptIds = new LinkedHashSet<>();
        int reminders = 0;
        for (var job : soon) {
            jobIds.add(job.id());
            if (job.attemptId() != null) attemptIds.add(job.attemptId());
            if (!reminded.add(job.attemptId() != null ? job.attemptId() : job.id())) continue;
            UUID eventId = UUID.randomUUID();
            var event = new GradingDueSoonV1(eventId, GradingDueSoonV1.EVENT_TYPE, now, job.examinerId(), job.id(),
                    job.skill(), job.chosenAt().plus(ExaminerAssignmentPolicy.DEADLINE));
            outbox.save(OutboxEvent.pending(eventId, AGGREGATE_TYPE, job.id().toString(), GradingDueSoonV1.EVENT_TYPE,
                    toJson(event), now));
            reminders++;
        }
        store.markReminded(jobIds, attemptIds, now);
        return reminders;
    }

    private String toJson(GradingDueSoonV1 event) {
        try {
            return json.writeValueAsString(event);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("The due-soon reminder cannot be written", failure);
        }
    }
}
