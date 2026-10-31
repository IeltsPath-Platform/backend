package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.CreateLearnerSubmissionCommand;
import com.ieltspath.assessment.application.result.LearnerSubmissionResult;
import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.assessment.domain.entity.LearnerSubmission;
import com.ieltspath.assessment.domain.exception.*;
import com.ieltspath.assessment.domain.repository.*;
import com.ieltspath.assessment.domain.vo.SubmissionStatus;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.key.InvalidObjectKeyException;
import com.ieltspath.commonstorage.key.ObjectKeys;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class CreateLearnerSubmissionUseCase {
    private final LearnerSubmissionRepository submissions;
    private final AttemptItemRepository items;
    private final AttemptSectionRepository sections;
    private final AssessmentAttemptRepository attempts;
    private final ObjectStorage storage;
    private final SpeakingRecordingProperties speakingRecordings;
    public CreateLearnerSubmissionUseCase(LearnerSubmissionRepository submissions, AttemptItemRepository items,
                                          AttemptSectionRepository sections, AssessmentAttemptRepository attempts,
                                          @Lazy ObjectStorage storage, SpeakingRecordingProperties speakingRecordings) {
        this.submissions=submissions; this.items=items; this.sections=sections; this.attempts=attempts;
        this.storage=storage; this.speakingRecordings=speakingRecordings;
    }
    @Transactional
    public LearnerSubmissionResult execute(CreateLearnerSubmissionCommand c) {
        if (c.skill() != com.ieltspath.assessment.domain.vo.Skill.WRITING && c.skill() != com.ieltspath.assessment.domain.vo.Skill.SPEAKING) throw new InvalidAssessmentStateException("Only writing or speaking submissions are supported");
        if ((c.textPayload() == null) == (c.audioReference() == null)) throw new InvalidAssessmentStateException("Exactly one text or audio payload is required");
        if (c.skill() == com.ieltspath.assessment.domain.vo.Skill.SPEAKING) {
            String reference = c.audioReference();
            if (reference.startsWith("local-recording:")) throw invalidRecording();
            try {
                ObjectKeys.requireOwnedBy(reference, "speaking", c.userId());
            } catch (InvalidObjectKeyException invalidKey) {
                throw invalidRecording();
            }
            if (!storage.exists(speakingRecordings.getBucket(), reference)) throw invalidRecording();
        }
        if (c.attemptItemId() != null) {
            var item=items.findById(c.attemptItemId()).orElseThrow(()->new AssessmentNotFoundException("Attempt item not found"));
            var section=sections.findById(item.attemptSectionId()).orElseThrow(()->new AssessmentNotFoundException("Attempt section not found"));
            attempts.findByIdAndUserId(section.attemptId(),c.userId()).orElseThrow(()->new AssessmentAccessDeniedException("Attempt item does not belong to current user"));
        }
        var existing=submissions.findByUserIdAndSubmissionKey(c.userId(),c.submissionKey());
        var s=existing.orElseGet(()->submissions.save(new LearnerSubmission(UUID.randomUUID(),c.userId(),c.attemptItemId(),c.promptSnapshot(),c.skill(),c.textPayload(),c.audioReference(),SubmissionStatus.SUBMITTED,c.submissionKey(),Instant.now())));
        return new LearnerSubmissionResult(s.id(),s.userId(),s.attemptItemId(),s.skill(),s.status(),s.submissionKey(),s.submittedAt());
    }

    private static InvalidAssessmentStateException invalidRecording() {
        return new InvalidAssessmentStateException("Speaking recording is invalid or unavailable");
    }
}
