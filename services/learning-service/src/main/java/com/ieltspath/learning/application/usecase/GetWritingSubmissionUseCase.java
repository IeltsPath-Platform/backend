package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.application.service.WritingSubmissionViewAssembler;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Only the owner reads a submission; another learner's id is not found. */
@Service
public class GetWritingSubmissionUseCase {
    private final WritingSubmissionRepository essays;
    private final WritingSubmissionViewAssembler view;

    public GetWritingSubmissionUseCase(WritingSubmissionRepository essays, WritingSubmissionViewAssembler view) {
        this.essays = essays;
        this.view = view;
    }

    public WritingSubmissionResult execute(UUID userId, UUID submissionId) {
        return essays.findOwned(submissionId, userId).map(view::assemble)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));
    }
}
