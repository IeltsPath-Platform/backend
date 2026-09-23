package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.CreateLearnerSubmissionCommand;
import com.group01.assessment.application.result.LearnerSubmissionResult;
import com.group01.assessment.domain.entity.LearnerSubmission;
import com.group01.assessment.domain.exception.*;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.SubmissionStatus;
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
    public CreateLearnerSubmissionUseCase(LearnerSubmissionRepository submissions, AttemptItemRepository items, AttemptSectionRepository sections, AssessmentAttemptRepository attempts) { this.submissions=submissions; this.items=items; this.sections=sections; this.attempts=attempts; }
    @Transactional
    public LearnerSubmissionResult execute(CreateLearnerSubmissionCommand c) {
        if (c.skill() != com.group01.assessment.domain.vo.Skill.WRITING && c.skill() != com.group01.assessment.domain.vo.Skill.SPEAKING) throw new InvalidAssessmentStateException("Only writing or speaking submissions are supported");
        if ((c.textPayload() == null) == (c.audioReference() == null)) throw new InvalidAssessmentStateException("Exactly one text or audio payload is required");
        if (c.attemptItemId() != null) {
            var item=items.findById(c.attemptItemId()).orElseThrow(()->new AssessmentNotFoundException("Attempt item not found"));
            var section=sections.findById(item.attemptSectionId()).orElseThrow(()->new AssessmentNotFoundException("Attempt section not found"));
            attempts.findByIdAndUserId(section.attemptId(),c.userId()).orElseThrow(()->new AssessmentAccessDeniedException("Attempt item does not belong to current user"));
        }
        var existing=submissions.findByUserIdAndSubmissionKey(c.userId(),c.submissionKey());
        var s=existing.orElseGet(()->submissions.save(new LearnerSubmission(UUID.randomUUID(),c.userId(),c.attemptItemId(),c.promptSnapshot(),c.skill(),c.textPayload(),c.audioReference(),SubmissionStatus.SUBMITTED,c.submissionKey(),Instant.now())));
        return new LearnerSubmissionResult(s.id(),s.userId(),s.attemptItemId(),s.skill(),s.status(),s.submissionKey(),s.submittedAt());
    }
}
