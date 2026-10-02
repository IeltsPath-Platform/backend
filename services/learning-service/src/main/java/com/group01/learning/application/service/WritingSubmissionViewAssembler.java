package com.group01.learning.application.service;

import com.group01.learning.application.result.WritingSubmissionResult;
import com.group01.learning.domain.aggregate.WritingSubmission;
import com.group01.learning.domain.repository.WritingSubmissionRepository;
import com.group01.learning.domain.vo.WritingSubmissionStatus;
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
            boolean showSample = Boolean.TRUE.equals(submission.passed())
                    || essays.blockPassed(submission.userId(), submission.blockId(), submission.id());
            return new WritingSubmissionResult(submission.id(), status, task, submission.wordCount(),
                    submission.overallBand(), submission.passed(), submission.grade(), submission.pointCost(),
                    showSample ? submission.prompt().sampleAnswer() : null, null, null);
        }
        if (submission.status() == WritingSubmissionStatus.PAYMENT_PENDING) {
            return new WritingSubmissionResult(submission.id(), status, task, null, null, null, null,
                    null, null, submission.failureCode(), null);
        }
        return new WritingSubmissionResult(submission.id(), status, task, null, null, null, null, null,
                null, null, submission.failureCode());
    }
}
