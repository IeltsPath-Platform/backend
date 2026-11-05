package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/** Queues the submission for grading once every question has its recording. Completing twice is harmless. */
@Service
public class CompleteSpeakingSubmissionUseCase {
    private final SpeakingSubmissionRepository submissions;
    private final LearnerLock lock;
    private final TransactionTemplate transaction;
    private final LearningContentClient content;
    private final ObjectMapper json;
    private final Clock clock = Clock.systemUTC();

    public CompleteSpeakingSubmissionUseCase(SpeakingSubmissionRepository submissions, LearnerLock lock,
                                             PlatformTransactionManager transactionManager) {
        this(submissions, lock, transactionManager, null, new ObjectMapper());
    }

    @Autowired
    public CompleteSpeakingSubmissionUseCase(SpeakingSubmissionRepository submissions, LearnerLock lock,
                                             PlatformTransactionManager transactionManager,
                                             LearningContentClient content, ObjectMapper json) {
        this.submissions = submissions;
        this.lock = lock;
        this.transaction = new TransactionTemplate(transactionManager);
        this.content = content;
        this.json = json;
    }

    public SpeakingSubmissionResult execute(UUID userId, UUID submissionId) {
        SpeakingSubmission existing = submissions.findOwned(submissionId, userId)
                .orElseThrow(() -> new LearningRequestException(404, "SPEAKING_SUBMISSION_NOT_FOUND",
                        "Speaking submission not found"));
        Map<String, Object> rubricSnapshot = null;
        if (existing.status() == SpeakingSubmissionStatus.UPLOADING && existing.hasAllRecordings()
                && existing.rubricSnapshot() == null
                && content != null) {
            LearningContentClient.Rubric rubric = content.getActiveRubric("SPEAKING");
            rubricSnapshot = json.convertValue(rubric, new TypeReference<>() { });
        }
        Map<String, Object> snapshot = rubricSnapshot;
        return transaction.execute(status -> {
            lock.lock(userId);
            SpeakingSubmission submission = submissions.findForUpdate(submissionId)
                    .filter(found -> found.userId().equals(userId))
                    .orElseThrow(() -> new LearningRequestException(404, "SPEAKING_SUBMISSION_NOT_FOUND",
                            "Speaking submission not found"));
            if (submission.status() == SpeakingSubmissionStatus.UPLOADING) {
                if (!submission.hasAllRecordings()) {
                    throw new LearningRequestException(422, "SPEAKING_RECORDINGS_MISSING",
                            submission.missingRecordings() + " question(s) have no recording yet", submissionId);
                }
                if (snapshot != null) submission.saveRubricSnapshot(snapshot);
                submission.complete(clock.instant());
                submissions.save(submission);
            } else if (submission.status() == SpeakingSubmissionStatus.ABANDONED) {
                throw new LearningRequestException(409, "SPEAKING_SUBMISSION_ABANDONED",
                        "This upload was given up; start a new submission", submissionId);
            }
            return SpeakingSubmissionResult.from(submission);
        });
    }
}
