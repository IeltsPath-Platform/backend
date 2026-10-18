package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The learner rewrites a graded lesson essay and sends it again. The revision goes through the same submission, LLM
 * grading, daily limit and point charge as any essay and is linked to the essay it improves; the grader is not told about
 * the earlier version. Only the newest graded essay of a chain can be revised, three times at most.
 */
@Service
public class SubmitEssayRevisionUseCase {
    private final WritingSubmissionRepository essays;
    private final SubmitLessonEssayUseCase submit;

    public SubmitEssayRevisionUseCase(WritingSubmissionRepository essays, SubmitLessonEssayUseCase submit) {
        this.essays = essays;
        this.submit = submit;
    }

    public WritingSubmissionResult execute(UUID userId, UUID submissionId, UUID requestId, String essayText) {
        // A resend of a revision already stored continues it, whatever has happened to the chain since.
        var sent = essays.findByRequestId(requestId);
        if (sent.isPresent()) {
            WritingSubmission existing = sent.get();
            if (!existing.userId().equals(userId) || !submissionId.equals(existing.previousSubmissionId())) {
                throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
            }
            return submit.execute(userId, existing.lessonId(), existing.blockId(), requestId, essayText, null);
        }
        WritingSubmission previous = essays.findOwned(submissionId, userId).orElseThrow(
                () -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));
        if (previous.blockId() == null) {
            throw new LearningRequestException(409, "NOT_REVISABLE", "Only a lesson essay can be revised");
        }
        if (previous.status() != WritingSubmissionStatus.GRADED) {
            throw new LearningRequestException(409, "NOT_REVISABLE", "Only a graded essay can be revised");
        }
        if (previous.revisionLimitReached()) {
            throw new LearningRequestException(409, "REVISION_LIMIT", "This essay has no revisions left");
        }
        if (essays.hasActiveRevision(previous.id())) {
            throw new LearningRequestException(409, "REVISION_NOT_LATEST",
                    "This essay has already been revised; revise the newest version");
        }
        return submit.execute(userId, previous.lessonId(), previous.blockId(), requestId, essayText, previous);
    }
}
