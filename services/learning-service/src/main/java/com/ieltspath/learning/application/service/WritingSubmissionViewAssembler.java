package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.service.WritingResultView;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Service;

/** Controls which grading details the learner has earned access to. */
@Service
public class WritingSubmissionViewAssembler {
    private final WritingSubmissionRepository essays;

    public WritingSubmissionViewAssembler(WritingSubmissionRepository essays) {
        this.essays = essays;
    }

    public WritingSubmissionResult assemble(WritingSubmission submission) {
        String task = submission.prompt().task();
        String status = submission.status().name();
        if (submission.status() == WritingSubmissionStatus.GRADED) {
            // Practice and free essays show their sample once graded; a lesson essay waits until its block is passed.
            boolean showSample = submission.practiceAttemptId() != null || submission.isFree()
                    || Boolean.TRUE.equals(submission.passed())
                    || essays.blockPassed(submission.userId(), submission.blockId(), submission.id());
            return new WritingSubmissionResult(submission.id(), status, task, submission.wordCount(),
                    submission.overallBand(), submission.passed(), submission.grade(), submission.pointCost(),
                    showSample ? submission.prompt().sampleAnswer() : null, null, null, submission.essayText(),
                    revision(submission));
        }
        if (submission.status() == WritingSubmissionStatus.AWAITING_EXAMINER) {
            // No grade lives here: the examiner's review is read from Assessment (GET .../human-review).
            return new WritingSubmissionResult(submission.id(), status, task, submission.wordCount(), null, null, null,
                    null, null, null, null, submission.essayText());
        }
        if (submission.status() == WritingSubmissionStatus.PAYMENT_PENDING) {
            return new WritingSubmissionResult(submission.id(), status, task, null, null, null, null,
                    null, null, submission.failureCode(), null);
        }
        return new WritingSubmissionResult(submission.id(), status, task, null, null, null, null, null,
                null, null, submission.failureCode());
    }

    /** One read of the version before, by id; an essay that is not a revision, or whose earlier version is gone, has none. */
    private WritingSubmissionResult.Revision revision(WritingSubmission submission) {
        if (submission.previousSubmissionId() == null || submission.grade() == null) return null;
        return essays.findOwned(submission.previousSubmissionId(), submission.userId())
                .map(WritingSubmission::grade)
                .map(previous -> {
                    var delta = WritingResultView.revisionDelta(previous, submission.grade());
                    return new WritingSubmissionResult.Revision(submission.previousSubmissionId(), delta.fixedCodes(),
                            delta.repeatedCodes(), delta.newCodes());
                }).orElse(null);
    }
}
