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
 * Marks a section of an in-progress attempt as finished. Its items then take no more responses, so a learner cannot
 * reopen a part they handed in. Completing an already completed section changes nothing.
 */
@Service
public class CompleteAttemptSectionUseCase {
    private final AssessmentAttemptRepository attempts;
    private final AttemptSectionRepository sections;

    public CompleteAttemptSectionUseCase(AssessmentAttemptRepository attempts, AttemptSectionRepository sections) {
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
        if (section.completed()) return;
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new InvalidAssessmentStateException("Only in-progress attempts can complete a section");
        }
        sections.save(section.complete(Instant.now()));
    }
}
