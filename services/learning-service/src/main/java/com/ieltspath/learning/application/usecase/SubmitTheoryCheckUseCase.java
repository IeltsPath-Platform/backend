package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Question;
import com.ieltspath.learning.application.port.ReviewTheoryCheckLog;
import com.ieltspath.learning.application.result.SubmissionResult;
import com.ieltspath.learning.application.result.TheoryCheckResult;
import com.ieltspath.learning.application.service.AnswerSheet;
import com.ieltspath.learning.application.service.TheoryFocus;
import com.ieltspath.learning.domain.aggregate.ReviewItem;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.AnswerSpecGrader;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grades the quick check of a review at the theory stage and returns the review to practice, right or wrong. The
 * answers are shown; no mastery evidence is written, because the questions come from the lesson the learner has done.
 */
@Service
public class SubmitTheoryCheckUseCase {
    private final LearningContentClient content;
    private final LearnerLock lock;
    private final ReviewItemRepository reviews;
    private final ReviewTheoryCheckLog checks;
    private final TheoryFocus theory = new TheoryFocus();
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public SubmitTheoryCheckUseCase(LearningContentClient content, LearnerLock lock, ReviewItemRepository reviews,
                                    ReviewTheoryCheckLog checks) {
        this.content = content;
        this.lock = lock;
        this.reviews = reviews;
        this.checks = checks;
    }

    @Transactional
    public TheoryCheckResult execute(UUID userId, UUID reviewId, SubmitExerciseCommand command) {
        lock.lock(userId);
        var saved = checks.find(command.requestId());
        if (saved.isPresent()) {
            if (!saved.get().userId().equals(userId) || !saved.get().reviewId().equals(reviewId)) {
                throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
            }
            return saved.get().response();
        }
        ReviewItem review = reviews.findOwned(userId, reviewId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
        if (review.status() != ReviewStatus.PENDING || review.stage() != ReviewStage.THEORY) {
            throw new LearningRequestException(409, "THEORY_NOT_REQUIRED", "This review is not at the theory stage");
        }
        List<Question> questions = theory.of(content.getLesson(review.lessonId()), review.knowledgePointId())
                .quickCheck();
        List<UUID> ids = questions.stream().map(Question::questionVersionId).toList();
        List<SubmissionResult.AnswerResult> results = new ArrayList<>();
        int correct = 0;
        if (questions.isEmpty()) {
            if (!command.answers().isEmpty()) {
                throw new LearningRequestException(422, "INVALID_ANSWERS", "This review has no quick-check questions");
            }
        } else {
            Map<UUID, Object> answers = AnswerSheet.require(ids, command.answers());
            for (Question question : questions) {
                var grade = grader.grade(question.answerSpec(), answers.get(question.questionVersionId()));
                if (!grade.gradable()) {
                    throw new LearningRequestException(422, "UNGRADABLE_EXERCISE",
                            "Quick check contains unsupported questions");
                }
                if (grade.correct()) correct++;
                results.add(new SubmissionResult.AnswerResult(question.questionVersionId(), grade.correct(),
                        grade.correctAnswer(), question.explanation()));
            }
        }
        review.completeTheory();
        reviews.save(review);
        TheoryCheckResult response = new TheoryCheckResult(reviewId, correct, questions.size(), List.copyOf(results),
                review.stage().name());
        checks.save(userId, reviewId, ids, command, response);
        return response;
    }
}
