package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.service.TrapEvidenceRule;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.application.result.ErrorTrapResult;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Adds trap evidence to a submission's normal evidence batch using only the local learning catalog. */
@Component
public class ErrorTrapEvidence {
    private final ErrorTrapCatalog traps;
    private final ErrorTrapReviews reviews;
    private final ErrorTypeTexts errorTypeTexts;

    @Autowired
    public ErrorTrapEvidence(ErrorTrapCatalog traps, ErrorTrapReviews reviews, ErrorTypeTexts errorTypeTexts) {
        this.traps = traps;
        this.reviews = reviews;
        this.errorTypeTexts = errorTypeTexts;
    }

    public ErrorTrapCatalog.ErrorTraps load() { return traps.load(); }
    public ErrorTrapCatalog.ErrorTraps loadIfNeeded(boolean hasDistractors) {
        return hasDistractors ? load() : null;
    }

    public void append(List<KnowledgeEvidence> evidence, Set<UUID> withoutMilestone, Map<String, Object> answerSpec,
                       boolean correct, String errorCode, UUID questionVersionId,
                       Function<UUID, UUID> referenceId, ErrorTrapCatalog.ErrorTraps catalog) {
        if (answerSpec == null || !(answerSpec.get("distractors") instanceof Map<?, ?>) || catalog == null) return;
        for (var outcome : TrapEvidenceRule.outcomes(answerSpec, correct, errorCode, catalog.trapKpByCode())) {
            evidence.add(KnowledgeEvidence.of(outcome.trapKnowledgePointId(), outcome.correct(), EvidenceSource.ERROR_TRAP,
                    referenceId.apply(outcome.trapKnowledgePointId())).forQuestion(questionVersionId));
            withoutMilestone.add(outcome.trapKnowledgePointId());
        }
    }

    /** Trigger parent reviews only after the caller has stored evidence and refreshed mastery. */
    public void createReviews(UUID userId, UUID submissionLessonId, List<KnowledgeEvidence> evidence) {
        if (evidence == null || evidence.isEmpty()) return;
        Set<UUID> trapKps = evidence.stream().filter(answer -> answer.source() == EvidenceSource.ERROR_TRAP)
                .map(KnowledgeEvidence::knowledgePointId).collect(Collectors.toSet());
        reviews.create(userId, submissionLessonId, trapKps);
    }

    /** Resolves all learner-facing labels with one Content lookup for a submission. */
    public Map<String, ErrorTypeTexts.Text> errorTexts(Collection<String> errorCodes) {
        if (errorCodes == null || errorCodes.stream().noneMatch(java.util.Objects::nonNull)) {
            return Map.of();
        }
        return errorTypeTexts.byCode();
    }

    public ErrorTrapResult feedback(String code, Map<String, ErrorTypeTexts.Text> texts) {
        if (code == null || texts == null) return null;
        ErrorTypeTexts.Text text = texts.get(code);
        return text == null ? null : new ErrorTrapResult(code, text.name(), text.fixHint());
    }

    public PracticeSubmission.ErrorTrap practiceFeedback(String code, Map<String, ErrorTypeTexts.Text> texts) {
        if (code == null || texts == null) return null;
        ErrorTypeTexts.Text text = texts.get(code);
        return text == null ? null : new PracticeSubmission.ErrorTrap(code, text.name(), text.fixHint());
    }
}
