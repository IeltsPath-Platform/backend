package com.group01.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.assessment.application.result.LearnerAssessmentResult;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.entity.ItemResult;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import com.group01.assessment.domain.repository.AssessmentResultRepository;
import com.group01.assessment.domain.repository.AttemptItemRepository;
import com.group01.assessment.domain.repository.ItemResultRepository;
import com.group01.assessment.domain.service.AnswerSpecGrader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
    private final ObjectMapper json;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public GetAssessmentResultUseCase(AssessmentAttemptRepository attempts, AssessmentResultRepository results,
                                      AttemptItemRepository attemptItems, ItemResultRepository itemResults,
                                      ObjectMapper json) {
        this.attempts = attempts;
        this.results = results;
        this.attemptItems = attemptItems;
        this.itemResults = itemResults;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public LearnerAssessmentResult execute(UUID userId, UUID attemptId) {
        attempts.findByIdAndUserId(attemptId, userId)
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

        return new LearnerAssessmentResult(result.id(), result.attemptId(), result.resultVersion(), result.status(),
                result.completedAt(), score.doubleValue(), maxScore.doubleValue(), percent, itemViews,
                percent >= SOLUTIONS_PERCENT ? solutions(items) : null);
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
