package com.ieltspath.assessment.application.service;

import com.ieltspath.assessment.application.port.ExaminerAssignmentStore;
import com.ieltspath.assessment.application.port.ExaminerDirectory;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.ExaminerChangeNotAllowedException;
import com.ieltspath.assessment.domain.exception.ExaminerUnavailableException;
import com.ieltspath.assessment.domain.exception.GradingCancelNotAllowedException;
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
        if (store.overdueOpen(examinerId, overdueBefore()) >= ExaminerAssignmentPolicy.MAX_OVERDUE_OPEN) {
            throw new ExaminerUnavailableException(ExaminerUnavailableException.Reason.OVERDUE_BACKLOG);
        }
        if (store.receivedSince(examinerId, startOfToday()) + slots > examiner.dailyLimit()) {
            throw new ExaminerUnavailableException(ExaminerUnavailableException.Reason.FULL);
        }
    }

    /** Jobs the examiner was chosen for at or before this moment are late. */
    private static Instant overdueBefore() {
        return Instant.now().minus(ExaminerAssignmentPolicy.DEADLINE);
    }

    /**
     * Which of the examiners cannot take any more submissions now, for the learner's list. Examiners that are unknown are
     * left out. {@code full} is true when they cannot take one: they stopped accepting, reached the daily limit, or have
     * too many late jobs ({@code backlog}, which the list words differently).
     */
    @Transactional(readOnly = true)
    public List<Availability> availability(Collection<UUID> examinerIds) {
        List<UUID> ids = examinerIds.stream().distinct().toList();
        Map<UUID, Integer> received = store.receivedSince(ids, startOfToday());
        Map<UUID, Integer> late = store.overdueOpen(ids, overdueBefore());
        // One call to the user service for the whole list, however many examiners it holds.
        Map<UUID, ExaminerDirectory.Examiner> known = directory.findAll(ids);
        return ids.stream().filter(known::containsKey).map(id -> {
            ExaminerDirectory.Examiner examiner = known.get(id);
            boolean backlog = late.getOrDefault(id, 0) >= ExaminerAssignmentPolicy.MAX_OVERDUE_OPEN;
            boolean full = !examiner.acceptingSubmissions() || received.getOrDefault(id, 0) >= examiner.dailyLimit();
            return new Availability(id, full || backlog, backlog);
        }).toList();
    }

    public record Availability(UUID examinerId, boolean full, boolean backlog) {}

    /**
     * The learner gives up a late job whose examiner can no longer be changed. The job closes, so nobody can grade it any
     * more; giving the credit back is Learning's step. A job already cancelled answers the same again.
     */
    @Transactional
    public void cancel(UUID learnerId, UUID jobId) {
        ExaminerAssignmentStore.JobRef job = store.lockJob(jobId)
                .filter(found -> found.learnerId().equals(learnerId))
                .orElseThrow(() -> new AssessmentNotFoundException("Grading job not found"));
        if ("CANCELLED".equals(job.status())) return;
        if (!ExaminerAssignmentPolicy.isOpen(job.status())) {
            throw new RevisionConflictException("Grading of this submission is already finished");
        }
        if (job.requestedExaminerId() == null || !ExaminerAssignmentPolicy.isOverdue(job.requestedAt(), Instant.now())) {
            throw new GradingCancelNotAllowedException(GradingCancelNotAllowedException.Reason.NOT_OVERDUE);
        }
        if (job.examinerChanges() < ExaminerAssignmentPolicy.MAX_CHANGES) {
            throw new GradingCancelNotAllowedException(GradingCancelNotAllowedException.Reason.CHANGES_LEFT);
        }
        store.cancel(job.id());
    }

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
                    job.band(), job.completedAt(), ExaminerAssignmentPolicy.canCancel(state, job.examinerChanges()));
        }).toList();
    }

    /**
     * {@code reviewSubmissionId} is the review id Learning holds for a Practice or Lesson essay; null for exam attempts.
     * {@code band} and {@code completedAt} are set once the examiner finished a standalone review, so the learner's list of
     * graded essays needs no second call. {@code canCancel} is true for a late job whose examiner can no longer be changed.
     */
    public record MyJobView(UUID id, UUID reviewSubmissionId, String skill, String source,
                            ExaminerAssignmentPolicy.State state, UUID examinerId, Instant requestedAt, Instant deadline,
                            boolean canChoose, int changesLeft, Instant createdAt, java.math.BigDecimal band,
                            Instant completedAt, boolean canCancel) {}
}
