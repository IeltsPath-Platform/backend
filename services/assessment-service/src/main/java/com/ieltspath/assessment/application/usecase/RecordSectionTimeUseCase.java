package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stores how many seconds the learner was active in each section of an attempt, so the result can show where the time
 * went. Only an in-progress attempt of the caller takes it; sections of other attempts are ignored.
 */
@Service
public class RecordSectionTimeUseCase {
    public record SectionTime(UUID sectionId, int seconds) {}

    private final AssessmentAttemptRepository attempts;
    private final AttemptSectionRepository sections;

    public RecordSectionTimeUseCase(AssessmentAttemptRepository attempts, AttemptSectionRepository sections) {
        this.attempts = attempts;
        this.sections = sections;
    }

    @Transactional
    public void execute(UUID userId, UUID attemptId, List<SectionTime> times) {
        if (times == null || times.isEmpty()) return;
        var attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) return;
        Map<UUID, Integer> secondsBySection = times.stream().collect(Collectors.toMap(
                SectionTime::sectionId, SectionTime::seconds, Math::max));
        for (var section : sections.findByAttemptId(attemptId)) {
            Integer seconds = secondsBySection.get(section.id());
            if (seconds != null) sections.save(section.withTimeSpent(seconds));
        }
    }
}
