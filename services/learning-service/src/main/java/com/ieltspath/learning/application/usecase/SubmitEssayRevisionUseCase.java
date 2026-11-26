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
                throw new LearningRequestException(409, "REQUEST_CONFLICT", "Mã yêu cầu này đã được sử dụng cho một bài nộp khác.");
            }
            return submit.execute(userId, existing.lessonId(), existing.blockId(), requestId, essayText, null);
        }
        WritingSubmission previous = essays.findOwned(submissionId, userId).orElseThrow(
                () -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài nộp."));
        if (previous.blockId() == null) {
            throw new LearningRequestException(409, "NOT_REVISABLE", "Chỉ có thể chỉnh sửa lại bài viết thuộc bài học.");
        }
        if (previous.status() != WritingSubmissionStatus.GRADED) {
            throw new LearningRequestException(409, "NOT_REVISABLE", "Chỉ có thể chỉnh sửa lại bài viết đã được chấm.");
        }
        if (previous.revisionLimitReached()) {
            throw new LearningRequestException(409, "REVISION_LIMIT", "Bài viết này đã hết lượt chỉnh sửa.");
        }
        if (essays.hasActiveRevision(previous.id())) {
            throw new LearningRequestException(409, "REVISION_NOT_LATEST",
                    "Bài viết này đã có bản chỉnh sửa mới hơn; vui lòng sửa trên bản mới nhất.");
        }
        return submit.execute(userId, previous.lessonId(), previous.blockId(), requestId, essayText, previous);
    }
}
