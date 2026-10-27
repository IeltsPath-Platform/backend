package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.application.service.EssayPrompts;
import com.ieltspath.learning.application.service.EssaySubmissionFlow;
import com.ieltspath.learning.application.service.FreeWritingPrompts;
import com.ieltspath.learning.application.service.WritingSubmissionViewAssembler;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
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
 * Grades an essay written for a prompt of the practice library (free writing or the step-by-step guide), with the same
 * paid flow as a lesson essay: graded outside any transaction, charged once, resumable with the same
 * {@code requestId}. The essay belongs to no lesson, so nothing else is recorded.
 */
@Service
public class SubmitFreeWritingUseCase {
    private final FreeWritingPrompts prompts;
    private final LearnerLock lock;
    private final WritingSubmissionRepository essays;
    private final EssaySubmissionFlow flow;
    private final TransactionTemplate transaction;
    private final WritingSubmissionViewAssembler view;
    private final Clock clock = Clock.systemUTC();

    public SubmitFreeWritingUseCase(FreeWritingPrompts prompts, LearnerLock lock,
                                    WritingSubmissionRepository essays, EssaySubmissionFlow flow,
                                    PlatformTransactionManager transactionManager,
                                    WritingSubmissionViewAssembler view) {
        this.prompts = prompts;
        this.lock = lock;
        this.essays = essays;
        this.flow = flow;
        this.transaction = new TransactionTemplate(transactionManager);
        this.view = view;
    }

    public WritingSubmissionResult execute(UUID userId, UUID packageVersionId, UUID questionVersionId, UUID requestId,
                                           String essayText) {
        int words = flow.validate(essayText);

        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        if (existing.isPresent()) {
            WritingSubmission submission = existing.get();
            if (!submission.userId().equals(userId) || !submission.isFree()
                    || !questionVersionId.equals(submission.prompt().questionVersionId())) {
                throw EssaySubmissionFlow.requestConflict();
            }
            if (submission.status() == WritingSubmissionStatus.GRADED) return view.assemble(submission);
            if (submission.status() == WritingSubmissionStatus.PAYMENT_PENDING) return charge(submission);
        }
        flow.requireGraderAvailable();

        Item item = prompts.find(packageVersionId, questionVersionId);
        flow.requireBalance();

        WritingSubmission grading = transaction.execute(status -> startGrading(userId, item, requestId, essayText,
                words));
        flow.grade(grading);
        return charge(grading);
    }

    private WritingSubmissionResult charge(WritingSubmission submission) {
        return flow.charge(submission, "free-writing:" + submission.userId() + ":" + submission.requestId(),
                "Writing practice grading", (charged, ledgerEntryId) -> transaction.execute(
                        status -> finish(charged, ledgerEntryId)));
    }

    private WritingSubmission startGrading(UUID userId, Item item, UUID requestId, String essayText, int words) {
        lock.lock(userId);
        Instant now = clock.instant();
        boolean gradingInProgress = false;
        for (WritingSubmission grading : essays.findGradingFree(userId, item.questionVersionId())) {
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
            WritingSubmission submission = WritingSubmission.startFree(UUID.randomUUID(), userId, requestId,
                    essayText, words, EssayPrompts.of(item), flow.pointCost(), now);
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
