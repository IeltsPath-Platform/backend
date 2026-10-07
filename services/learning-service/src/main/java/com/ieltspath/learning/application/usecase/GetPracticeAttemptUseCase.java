package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.application.service.PracticeAttemptViewAssembler;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetPracticeAttemptUseCase {
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final PracticeAttemptViewAssembler view;
    private final PracticeAccess access;

    public GetPracticeAttemptUseCase(PracticeAttemptRepository attempts, LearningContentClient content,
                                     PracticeAttemptViewAssembler view, PracticeAccess access) {
        this.attempts = attempts;
        this.content = content;
        this.view = view;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public Result execute(UUID userId, UUID attemptId) {
        var attempt = attempts.findOwned(userId, attemptId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));
        if (attempt.submitted()) return new Result(null, attempt.response());
        access.require(userId, attempt.lessonId());
        return new Result(view.assemble(attempt, content.getPackageVersion(attempt.packageVersionId())), null);
    }

    public record Result(PracticeAttemptView view, PracticeSubmission submission) {}
}
