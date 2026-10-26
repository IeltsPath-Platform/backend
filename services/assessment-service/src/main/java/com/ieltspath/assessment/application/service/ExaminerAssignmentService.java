package com.ieltspath.assessment.application.service;

import com.ieltspath.assessment.application.port.ExaminerAssignmentStore;
import com.ieltspath.assessment.application.port.ExaminerDirectory;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.ExaminerChangeNotAllowedException;
import com.ieltspath.assessment.domain.exception.ExaminerUnavailableException;
import com.ieltspath.assessment.domain.exception.RevisionConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Every human grading job belongs to the examiner the learner chose. This is where that choice is made and checked. */
@Service
public class ExaminerAssignmentService {
    private final ExaminerAssignmentStore store;
    private final ExaminerDirectory directory;
    private final ZoneId dayZone;
    private final GradingRequestAnnouncer announcer;

    public ExaminerAssignmentService(ExaminerAssignmentStore store, ExaminerDirectory directory, GradingRequestAnnouncer announcer,
                                     @Value("${assessment.examiner.day-timezone:Asia/Ho_Chi_Minh}") String dayTimezone) {
        this.store = store;
        this.directory = directory;
        this.announcer = announcer;
        this.dayZone = ZoneId.of(dayTimezone);
    }

    /** The start of the examiner's day: what they received before it no longer counts against today's limit. */
    private Instant startOfToday() {
        return LocalDate.now(dayZone).atStartOfDay(dayZone).toInstant();
    }

    /**
     * The examiner exists, accepts submissions and has not reached their limit of new submissions today, counting the
     * {@code slots} about to be given. The count is taken under a lock on the examiner, so two learners cannot both take
     * the last place.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireAvailable(UUID examinerId, int slots) {
        ExaminerDirectory.Examiner examiner = directory.find(examinerId)
                .orElseThrow(() -> new AssessmentNotFoundException("Examiner not found"));
        if (!examiner.acceptingSubmissions()) {
            throw new ExaminerUnavailableException(ExaminerUnavailableException.Reason.NOT_ACCEPTING);
        }
        store.lockExaminer(examinerId);
        if (store.receivedSince(examinerId, startOfToday()) + slots > examiner.dailyLimit()) {
            throw new ExaminerUnavailableException(ExaminerUnavailableException.Reason.FULL);
        }
    }

    /**
     * Which of the examiners cannot take any more submissions today, for the learner's list. Examiners that are unknown
     * are left out; a full examiner has reached the daily limit or stopped accepting.
     */
    @Transactional(readOnly = true)
    public List<Availability> availability(Collection<UUID> examinerIds) {
        Map<UUID, Integer> received = store.receivedSince(examinerIds, startOfToday());
        return examinerIds.stream().distinct().flatMap(id -> directory.find(id).stream().map(examiner ->
                new Availability(id, !examiner.acceptingSubmissions() || received.getOrDefault(id, 0) >= examiner.dailyLimit())))
                .toList();
    }

    public record Availability(UUID examinerId, boolean full) {}

    /**
     * The learner chooses the examiner of a job that waits for a choice, or replaces the examiner of a late job.
     * All open jobs of the same attempt move together, so one examiner grades the whole attempt.
     */
    @Transactional
    public MyJobView choose(UUID learnerId, UUID jobId, UUID examinerId) {
        ExaminerAssignmentStore.JobRef job = store.lockJob(jobId)
                .filter(found -> found.learnerId().equals(learnerId))
                .orElseThrow(() -> new AssessmentNotFoundException("Grading job not found"));
        if (!ExaminerAssignmentPolicy.isOpen(job.status())) {
            throw new RevisionConflictException("Grading of this submission is already finished");
        }
        boolean replacing = job.requestedExaminerId() != null;
        if (replacing) {
            if (examinerId.equals(job.requestedExaminerId())) {
                throw new ExaminerChangeNotAllowedException(ExaminerChangeNotAllowedException.Reason.SAME_EXAMINER);
            }
            if (!ExaminerAssignmentPolicy.isOverdue(job.requestedAt(), Instant.now())) {
                throw new ExaminerChangeNotAllowedException(ExaminerChangeNotAllowedException.Reason.NOT_OVERDUE);
            }
            if (job.examinerChanges() >= ExaminerAssignmentPolicy.MAX_CHANGES) {
                throw new ExaminerChangeNotAllowedException(ExaminerChangeNotAllowedException.Reason.LIMIT_REACHED);
            }
        }
        List<ExaminerAssignmentStore.JobRef> group = store.lockOpenGroup(job);
        requireAvailable(examinerId, group.size());
        store.assign(group.stream().map(ExaminerAssignmentStore.JobRef::id).toList(), examinerId, replacing);
        // One examiner grades the whole attempt, so the examiner is told once, not once per job.
        announcer.announce(examinerId, job.id(), job.skill(), Instant.now());
        return mine(learnerId).stream().filter(view -> view.id().equals(jobId)).findFirst().orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<MyJobView> mine(UUID learnerId) {
        Instant now = Instant.now();
        return store.listMine(learnerId).stream().map(job -> {
            var state = ExaminerAssignmentPolicy.stateOf(job.status(), job.requestedExaminerId() != null,
                    job.requestedAt(), now);
            return new MyJobView(job.id(), job.reviewSubmissionId(), job.skill(), job.source(), state, job.requestedExaminerId(),
                    job.requestedAt(), job.requestedAt() == null ? null
                            : job.requestedAt().plus(ExaminerAssignmentPolicy.DEADLINE),
                    ExaminerAssignmentPolicy.canChoose(state, job.examinerChanges()),
                    Math.max(0, ExaminerAssignmentPolicy.MAX_CHANGES - job.examinerChanges()), job.createdAt(),
                    job.band(), job.completedAt());
        }).toList();
    }

    /**
     * {@code reviewSubmissionId} is the review id Learning holds for a Practice or Lesson essay; null for exam attempts.
     * {@code band} and {@code completedAt} are set once the examiner finished a standalone review, so the learner's list of
     * graded essays needs no second call.
     */
    public record MyJobView(UUID id, UUID reviewSubmissionId, String skill, String source,
                            ExaminerAssignmentPolicy.State state, UUID examinerId, Instant requestedAt, Instant deadline,
                            boolean canChoose, int changesLeft, Instant createdAt, java.math.BigDecimal band,
                            Instant completedAt) {}
}
