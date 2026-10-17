package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.ieltspath.assessment.application.result.AssessmentAttemptResult;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.AttemptExpiredException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Submits an attempt and, when every item is objective, grades it in the same transaction. A repeated submit
 * returns the attempt unchanged and writes nothing. A late submit commits the EXPIRED state and then fails.
 * The time the learner spent in each section is stored in the same transaction, only when the submit goes through.
 */
@Service
public class SubmitAssessmentAttemptUseCase {
    private final AssessmentAttemptRepository repository;
    private final EnqueueGateEssayGradingService gateEssayGrading;
    private final AttemptSectionRepository sections;

    public SubmitAssessmentAttemptUseCase(AssessmentAttemptRepository repository,
                                          EnqueueGateEssayGradingService gateEssayGrading,
                                          AttemptSectionRepository sections) {
        this.repository = repository;
        this.gateEssayGrading = gateEssayGrading;
        this.sections = sections;
    }

    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public AssessmentAttemptResult execute(SubmitAssessmentAttemptCommand c) {
        var attempt = repository.findByIdAndUserId(c.attemptId(), c.userId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            return AssessmentAttemptResult.from(attempt);
        }
        try {
            attempt.submit(Instant.now());
        } catch (AttemptExpiredException expired) {
            repository.save(attempt);
            throw expired;
        }
        recordSectionTimes(c.attemptId(), c.sectionSeconds());
        var saved = repository.save(attempt);
        gateEssayGrading.enqueueOrGrade(saved);
        return AssessmentAttemptResult.from(saved);
    }

    /** Sections that do not belong to this attempt are ignored. */
    private void recordSectionTimes(UUID attemptId, List<SubmitAssessmentAttemptCommand.SectionTime> times) {
        if (times.isEmpty()) return;
        Map<UUID, Integer> secondsBySection = times.stream().collect(Collectors.toMap(
                SubmitAssessmentAttemptCommand.SectionTime::sectionId,
                SubmitAssessmentAttemptCommand.SectionTime::seconds, Math::max));
        var updated = sections.findByAttemptId(attemptId).stream()
                .filter(section -> secondsBySection.containsKey(section.id()))
                .map(section -> section.withTimeSpent(secondsBySection.get(section.id())))
                .toList();
        if (!updated.isEmpty()) sections.saveAll(updated);
    }
}
