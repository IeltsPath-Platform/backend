package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.OverdueNoticeStore;
import com.ieltspath.assessment.application.service.ExaminerAssignmentPolicy;
import com.ieltspath.assessment.domain.entity.OutboxEvent;
import com.ieltspath.assessment.domain.repository.OutboxEventRepository;
import com.ieltspath.events.assessment.GradingOverdueV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tells the learner, once, that the examiner they chose has not graded within the deadline, so they can choose
 * another. The announcement is an outbox row written in the same transaction that marks the job as announced: a crash
 * can neither lose the notice nor send it twice. One examiner grades a whole attempt, so an attempt is announced once.
 * Handing the job to another examiner clears the mark and the new deadline starts again.
 */
@Service
public class AnnounceOverdueGradingUseCase {
    static final String AGGREGATE_TYPE = "GradingJob";

    private final OverdueNoticeStore store;
    private final OutboxEventRepository outbox;
    private final ObjectMapper json;
    private final int batchSize;

    public AnnounceOverdueGradingUseCase(OverdueNoticeStore store, OutboxEventRepository outbox, ObjectMapper json,
                                         @Value("${assessment.examiner.overdue-notice.batch-size:100}") int batchSize) {
        this.store = store;
        this.outbox = outbox;
        this.json = json;
        this.batchSize = batchSize;
    }

    /** @return the number of notices recorded in this batch. */
    @Transactional
    public int execute(Instant now) {
        var late = store.claimUnannounced(now.minus(ExaminerAssignmentPolicy.DEADLINE), batchSize);
        if (late.isEmpty()) return 0;
        Set<Object> announced = new HashSet<>();
        Set<UUID> jobIds = new LinkedHashSet<>();
        Set<UUID> attemptIds = new LinkedHashSet<>();
        int notices = 0;
        for (var job : late) {
            jobIds.add(job.id());
            if (job.attemptId() != null) attemptIds.add(job.attemptId());
            if (!announced.add(job.attemptId() != null ? job.attemptId() : job.id())) continue;
            UUID eventId = UUID.randomUUID();
            var event = new GradingOverdueV1(eventId, GradingOverdueV1.EVENT_TYPE, now, job.learnerId(),
                    job.examinerId(), job.id(), job.skill(), job.chosenAt().plus(ExaminerAssignmentPolicy.DEADLINE));
            outbox.save(OutboxEvent.pending(eventId, AGGREGATE_TYPE, job.id().toString(), GradingOverdueV1.EVENT_TYPE,
                    toJson(event), now));
            notices++;
        }
        store.markAnnounced(jobIds, attemptIds, now);
        return notices;
    }

    private String toJson(GradingOverdueV1 event) {
        try {
            return json.writeValueAsString(event);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("The overdue notice cannot be written", failure);
        }
    }
}
