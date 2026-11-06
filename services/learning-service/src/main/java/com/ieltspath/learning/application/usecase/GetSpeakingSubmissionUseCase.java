package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetSpeakingSubmissionUseCase {
    private final SpeakingSubmissionRepository submissions;

    public GetSpeakingSubmissionUseCase(SpeakingSubmissionRepository submissions) {
        this.submissions = submissions;
    }

    public SpeakingSubmissionResult execute(UUID userId, UUID submissionId) {
        return submissions.findOwned(submissionId, userId).map(SpeakingSubmissionResult::from)
                .orElseThrow(() -> new LearningRequestException(404, "SPEAKING_SUBMISSION_NOT_FOUND",
                        "Speaking submission not found"));
    }
}
