package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.application.service.EssayPrompts;
import com.ieltspath.learning.application.service.EssaySubmissionFlow;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.application.service.WritingSubmissionViewAssembler;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.PracticeOrigin;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Sends an essay of a lesson's practice attempt to the examiner the learner chose, in place of the AI grade. The essay is
 * stored first as AWAITING_EXAMINER with its lesson, then the review is asked for: Access spends one Premium grading credit
 * and Assessment queues it for that examiner. Nothing is graded or charged in points here. The examiner's grade reaches
 * the learner's mastery when Assessment reports it (see {@link ApplyHumanReviewResultUseCase}), so the AI must not grade
 * the same question of the attempt as well: that would count one essay twice. A resend with the same {@code requestId}
 * continues the same essay, also to another examiner after a refusal.
 */
@Service
public class SubmitPracticeEssayToExaminerUseCase {
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final PracticeAccess access;
    private final LearnerLock lock;
    private final WritingSubmissionRepository essays;
    private final HumanReviewRequestRepository requests;
    private final EssaySubmissionFlow flow;
    private final HumanReviewUseCase reviews;
    private final WritingSubmissionViewAssembler view;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    public SubmitPracticeEssayToExaminerUseCase(PracticeAttemptRepository attempts, LearningContentClient content,
                                                PracticeAccess access, LearnerLock lock,
                                                WritingSubmissionRepository essays,
                                                HumanReviewRequestRepository requests, EssaySubmissionFlow flow,
                                                HumanReviewUseCase reviews, WritingSubmissionViewAssembler view,
                                                PlatformTransactionManager transactionManager) {
        this.attempts = attempts;
        this.content = content;
        this.access = access;
        this.lock = lock;
        this.essays = essays;
        this.requests = requests;
        this.flow = flow;
        this.reviews = reviews;
        this.view = view;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public WritingSubmissionResult execute(UUID userId, UUID attemptId, UUID questionVersionId, UUID requestId,
                                           String essayText, UUID examinerId) {
        if (examinerId == null) {
            throw new LearningRequestException(400, "EXAMINER_REQUIRED", "Choose the examiner who will grade the essay");
        }
        int words = flow.validate(essayText);
        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        WritingSubmission submission;
        if (existing.isPresent()) {
            submission = existing.get();
            if (!sameEssay(submission, userId, attemptId, questionVersionId)) throw EssaySubmissionFlow.requestConflict();
        } else {
            PracticeAttempt attempt = open(userId, attemptId);
            Item item = essayItem(attempt, questionVersionId);
            submission = transaction.execute(status -> store(userId, attemptId, item, requestId, essayText, words));
            if (!sameEssay(submission, userId, attemptId, questionVersionId)) throw EssaySubmissionFlow.requestConflict();
        }
        // No transaction is open: the credit is spent in Access and the review is made in Assessment.
        reviews.request(userId, submission.id(), examinerId);
        return view.assemble(submission);
    }

    private PracticeAttempt open(UUID userId, UUID attemptId) {
        PracticeAttempt attempt = attempts.findOwned(userId, attemptId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));
        if (attempt.submitted()) {
            throw new LearningRequestException(409, "ATTEMPT_SUBMITTED", "The practice attempt was already submitted");
        }
        if (attempt.origin() == PracticeOrigin.FREE) {
            throw new LearningRequestException(409, "FREE_PRACTICE_ESSAY_UNSUPPORTED", "Free practice does not take essays");
        }
        return attempt;
    }

    private Item essayItem(PracticeAttempt attempt, UUID questionVersionId) {
        return ItemGrading.orderedItems(content.getPackageVersion(attempt.packageVersionId())).stream()
                .filter(candidate -> candidate.questionVersionId().equals(questionVersionId))
                .filter(ItemGrading::isEssay).findFirst()
                .orElseThrow(() -> new LearningRequestException(409, "NOT_ESSAY_ITEM",
                        "This question is not an essay of the practice set"));
    }

    /** Stores the essay unless a concurrent request with the same id stored it first; that one is returned then. */
    private WritingSubmission store(UUID userId, UUID attemptId, Item item, UUID requestId, String essayText, int words) {
        lock.lock(userId);
        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        if (existing.isPresent()) return existing.get();
        PracticeAttempt attempt = open(userId, attemptId);
        access.require(userId, attempt.lessonId());
        requireNoOtherGrade(essays.latestForPractice(userId, attemptId).get(item.questionVersionId()));
        WritingSubmission submission = WritingSubmission.startPracticeForExaminer(UUID.randomUUID(), userId,
                attempt.lessonId(), attemptId, requestId, essayText, words, EssayPrompts.of(item), clock.instant());
        if (!essays.save(submission)) {
            throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
        }
        return submission;
    }

    /**
     * One grade per question of the attempt: an essay the AI graded or is grading, or one already with an examiner, keeps
     * the question. A failed AI grade, or an essay whose examiner request never went through, does not.
     */
    private void requireNoOtherGrade(WritingSubmission latest) {
        if (latest == null) return;
        switch (latest.status()) {
            case GRADED -> throw new LearningRequestException(409, "ESSAY_ALREADY_GRADED",
                    "This essay was already graded by the AI; ask a teacher from its result instead");
            case GRADING, PAYMENT_PENDING -> throw EssaySubmissionFlow.gradingInProgress();
            case AWAITING_EXAMINER -> {
                if (requests.findReviewId(latest.id()).isPresent()) throw examinerPending(latest.id());
            }
            default -> { }
        }
    }

    static LearningRequestException examinerPending(UUID submissionId) {
        return new LearningRequestException(409, "EXAMINER_REVIEW_PENDING",
                "This essay was already sent to an examiner who has not graded it yet", submissionId);
    }

    private static boolean sameEssay(WritingSubmission submission, UUID userId, UUID attemptId, UUID questionVersionId) {
        return submission.userId().equals(userId) && attemptId.equals(submission.practiceAttemptId())
                && submission.status() == WritingSubmissionStatus.AWAITING_EXAMINER
                && questionVersionId.equals(submission.prompt().questionVersionId());
    }
}
