package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Records when the learner first opens a section, so the time they spent on it can be shown once it is finished.
 * Opening it again, or opening a finished section, changes nothing.
 */
@Service
public class StartAttemptSectionUseCase {
    private final AssessmentAttemptRepository attempts;
    private final AttemptSectionRepository sections;

    public StartAttemptSectionUseCase(AssessmentAttemptRepository attempts, AttemptSectionRepository sections) {
        this.attempts = attempts;
        this.sections = sections;
    }

    @Transactional
    public void execute(UUID userId, UUID attemptId, UUID sectionId) {
        var attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        AttemptSection section = sections.findById(sectionId)
                .filter(candidate -> candidate.attemptId().equals(attemptId))
                .orElseThrow(() -> new AssessmentNotFoundException("Attempt section not found"));
        if (section.startedAt() != null || section.completed()) return;
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new InvalidAssessmentStateException("Only in-progress attempts can start a section");
        }
        sections.save(section.start(Instant.now()));
    }
}
