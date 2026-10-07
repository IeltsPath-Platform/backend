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
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Grades one essay of an open practice attempt, with the same paid flow as a lesson essay: graded outside any
 * transaction, charged once, resumable with the same {@code requestId}. The essay counts toward the attempt only when
 * the attempt is submitted, so nothing else is recorded here.
 */
@Service
public class SubmitPracticeEssayUseCase {
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final PracticeAccess access;
    private final LearnerLock lock;
    private final WritingSubmissionRepository essays;
    private final EssaySubmissionFlow flow;
    private final TransactionTemplate transaction;
    private final WritingSubmissionViewAssembler view;
    private final Clock clock = Clock.systemUTC();

    public SubmitPracticeEssayUseCase(PracticeAttemptRepository attempts, LearningContentClient content,
                                      PracticeAccess access, LearnerLock lock, WritingSubmissionRepository essays,
                                      EssaySubmissionFlow flow, PlatformTransactionManager transactionManager,
                                      WritingSubmissionViewAssembler view) {
        this.attempts = attempts;
        this.content = content;
        this.access = access;
        this.lock = lock;
        this.essays = essays;
        this.flow = flow;
        this.transaction = new TransactionTemplate(transactionManager);
        this.view = view;
    }

    public WritingSubmissionResult execute(UUID userId, UUID attemptId, UUID questionVersionId, UUID requestId,
                                           String essayText) {
        int words = flow.validate(essayText);

        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        if (existing.isPresent()) {
            WritingSubmission submission = existing.get();
            if (!submission.userId().equals(userId) || !attemptId.equals(submission.practiceAttemptId())
                    || !questionVersionId.equals(submission.prompt().questionVersionId())) {
                throw EssaySubmissionFlow.requestConflict();
            }
            if (submission.status() == WritingSubmissionStatus.GRADED) return view.assemble(submission);
            if (submission.status() == WritingSubmissionStatus.PAYMENT_PENDING) return charge(submission);
        }
        flow.requireGraderAvailable();

        PracticeAttempt attempt = owned(userId, attemptId);
        Item item = ItemGrading.orderedItems(content.getPackageVersion(attempt.packageVersionId())).stream()
                .filter(candidate -> candidate.questionVersionId().equals(questionVersionId))
                .filter(ItemGrading::isEssay).findFirst()
                .orElseThrow(() -> new LearningRequestException(409, "NOT_ESSAY_ITEM",
                        "This question is not an essay of the practice set"));
        flow.requireBalance();

        WritingSubmission grading = transaction.execute(status -> startGrading(userId, attemptId, item, requestId,
                essayText, words));
        flow.grade(grading);
        return charge(grading);
    }

    private PracticeAttempt owned(UUID userId, UUID attemptId) {
        PracticeAttempt attempt = attempts.findOwned(userId, attemptId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));
        if (attempt.submitted()) {
            throw new LearningRequestException(409, "ATTEMPT_SUBMITTED", "The practice attempt was already submitted");
        }
        return attempt;
    }

    private WritingSubmissionResult charge(WritingSubmission submission) {
        return flow.charge(submission, "practice-writing:" + submission.userId() + ":" + submission.requestId(),
                "Practice essay grading", (charged, ledgerEntryId) -> transaction.execute(
                        status -> finish(charged, ledgerEntryId)));
    }

    private WritingSubmission startGrading(UUID userId, UUID attemptId, Item item, UUID requestId, String essayText,
                                           int words) {
        lock.lock(userId);
        PracticeAttempt attempt = owned(userId, attemptId);
        access.require(userId, attempt.lessonId());
        Instant now = clock.instant();
        boolean gradingInProgress = false;
        for (WritingSubmission grading : essays.findGradingForPractice(userId, attemptId, item.questionVersionId())) {
            if (grading.isStale(now.minusSeconds(flow.staleGradingSeconds()))) {
                grading.abandon();
                if (!essays.save(grading)) gradingInProgress = true;
            } else {
                gradingInProgress = true;
            }
        }
        if (gradingInProgress) throw EssaySubmissionFlow.gradingInProgress();
        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        if (existing.isEmpty()) {
            WritingSubmission submission = WritingSubmission.startPractice(UUID.randomUUID(), userId,
                    attempt.lessonId(), attemptId, requestId, essayText, words, EssayPrompts.of(item),
                    flow.pointCost(), now);
            essays.save(submission);
            return submission;
        }
        WritingSubmission submission = existing.get();
        if (submission.status() != WritingSubmissionStatus.FAILED) throw EssaySubmissionFlow.gradingInProgress();
        submission.restart(now);
        if (!essays.save(submission)) throw EssaySubmissionFlow.gradingInProgress();
        return submission;
    }

    private WritingSubmissionResult finish(WritingSubmission charged, UUID ledgerEntryId) {
        lock.lock(charged.userId());
        WritingSubmission submission = essays.findForUpdate(charged.id()).orElseThrow();
        if (submission.status() == WritingSubmissionStatus.GRADED) return view.assemble(submission);
        submission.markGraded(ledgerEntryId);
        essays.save(submission);
        return view.assemble(submission);
    }
}
