package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ieltspath.assessment.application.result.LearnerAssessmentResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.repository.ItemResultRepository;
import com.ieltspath.assessment.domain.repository.SkillScoreRepository;
import com.ieltspath.assessment.domain.service.AnswerSpecGrader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.entity.SkillScore;
import com.ieltspath.assessment.domain.vo.AttemptType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The learner's view of their latest COMPLETED result. Correctness is always shown; correct answers and
 * explanations only once the learner reached the pass mark, so a failed final test cannot be replayed from its key.
 */
@Service
public class GetAssessmentResultUseCase {
    static final double SOLUTIONS_PERCENT = 70.0;

    private final AssessmentAttemptRepository attempts;
    private final AssessmentResultRepository results;
    private final AttemptItemRepository attemptItems;
    private final ItemResultRepository itemResults;
    private final AttemptSectionRepository sections;
    private final SkillScoreRepository skillScores;
    private final ObjectMapper json;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public GetAssessmentResultUseCase(AssessmentAttemptRepository attempts, AssessmentResultRepository results,
                                      AttemptItemRepository attemptItems, ItemResultRepository itemResults,
                                      AttemptSectionRepository sections, SkillScoreRepository skillScores,
                                      ObjectMapper json) {
        this.attempts = attempts;
        this.results = results;
        this.attemptItems = attemptItems;
        this.itemResults = itemResults;
        this.sections = sections;
        this.skillScores = skillScores;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public LearnerAssessmentResult execute(UUID userId, UUID attemptId) {
        var attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        var result = results.findLatestCompletedByAttemptId(attemptId)
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));
        List<AttemptItem> items = attemptItems.findByAttemptId(attemptId);
        Map<UUID, ItemResult> gradedByItem = itemResults.findByResultId(result.id()).stream()
                .collect(Collectors.toMap(ItemResult::attemptItemId, Function.identity()));

        BigDecimal score = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        List<LearnerAssessmentResult.Item> itemViews = new ArrayList<>(items.size());
        for (AttemptItem item : items) {
            ItemResult graded = gradedByItem.get(item.id());
            if (graded != null) {
                score = score.add(decimal(graded.score()));
                maxScore = maxScore.add(decimal(graded.maxScore()));
            }
            itemViews.add(new LearnerAssessmentResult.Item(item.id(), item.questionVersionId(),
                    graded == null ? null : graded.correct()));
        }
        double percent = maxScore.signum() == 0 ? 0.0
                : score.multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, RoundingMode.HALF_UP).doubleValue();

        boolean showSolutions = percent >= SOLUTIONS_PERCENT
                || attempt.getAttemptType() == com.ieltspath.assessment.domain.vo.AttemptType.OFFICIAL_PRACTICE;
        return new LearnerAssessmentResult(result.id(), result.attemptId(), result.resultVersion(), result.status(),
                result.completedAt(), score.doubleValue(), maxScore.doubleValue(), percent, itemViews,
                showSolutions ? solutions(items) : null,
                showSolutions ? sectionSolutions(attemptId) : null,
                practice(attempt, result.id(), items));
    }

    /** Only a practice test result carries timing and band; every other attempt keeps its former shape. */
    private LearnerAssessmentResult.Practice practice(AssessmentAttempt attempt, UUID resultId, List<AttemptItem> items) {
        if (attempt.getAttemptType() != AttemptType.OFFICIAL_PRACTICE) return null;
        Map<UUID, Long> questionCount = items.stream().collect(Collectors.groupingBy(AttemptItem::attemptSectionId,
                Collectors.counting()));
        List<LearnerAssessmentResult.SectionSummary> summaries = new ArrayList<>();
        boolean everySectionHasLimit = true;
        int limitTotal = 0;
        for (var section : sections.findByAttemptId(attempt.getId()).stream()
                .sorted(Comparator.comparingInt(AttemptSection::sortOrder)).toList()) {
            String title = null;
            Integer limit = null;
            try {
                var snapshot = section.snapshot() == null ? null : json.readTree(section.snapshot());
                if (snapshot != null) {
                    if (snapshot.path("title").isTextual()) title = snapshot.path("title").textValue();
                    if (snapshot.path("timeLimitSeconds").isInt()) limit = snapshot.path("timeLimitSeconds").intValue();
                }
            } catch (JsonProcessingException exception) {
                // A snapshot that is not valid JSON has neither title nor limit.
            }
            if (limit == null) everySectionHasLimit = false; else limitTotal += limit;
            summaries.add(new LearnerAssessmentResult.SectionSummary(section.id(), section.partNo(), title,
                    questionCount.getOrDefault(section.id(), 0L).intValue(), section.timeSpentSeconds(), limit));
        }
        Double band = attempt.getSkill() == null ? null : skillScores.findByResultId(resultId).stream()
                .filter(score -> score.skill() == attempt.getSkill()).map(SkillScore::band)
                .filter(Objects::nonNull).findFirst().orElse(null);
        return new LearnerAssessmentResult.Practice(attempt.getScope(), attempt.getSkill(), attempt.getMode(),
                attempt.getStartedAt(), attempt.getSubmittedAt(), everySectionHasLimit && !summaries.isEmpty() ? limitTotal : null,
                band, List.copyOf(summaries));
    }

    private List<LearnerAssessmentResult.SectionSolution> sectionSolutions(UUID attemptId) {
        var solutions = new ArrayList<LearnerAssessmentResult.SectionSolution>();
        for (var section : sections.findByAttemptId(attemptId)) {
            if (section.snapshot() == null) continue;
            try {
                var snapshot = json.readTree(section.snapshot());
                if (snapshot == null) continue;
                var transcript = snapshot.path("solution").path("transcript");
                if (transcript.isTextual()) {
                    solutions.add(new LearnerAssessmentResult.SectionSolution(section.id(), transcript.textValue()));
                }
            } catch (JsonProcessingException exception) {
                // Older snapshots without valid JSON have no section solution.
            }
        }
        return List.copyOf(solutions);
    }

    private List<LearnerAssessmentResult.Solution> solutions(List<AttemptItem> items) {
        return items.stream()
                .map(item -> {
                    var answer = AnswerSnapshot.parse(json, item.answerSnapshot());
                    String correctAnswer = answer.map(a -> grader.grade(a.answerSpec(), null).correctAnswer())
                            .orElse(null);
                    String explanation = answer.map(AnswerSnapshot::explanation).orElse(null);
                    return new LearnerAssessmentResult.Solution(item.id(), item.questionVersionId(), correctAnswer,
                            explanation);
                })
                .toList();
    }

    private static BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }
}
