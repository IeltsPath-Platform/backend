package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grades the quick check of a review at the theory stage and returns the review to practice, right or wrong. The
 * answers are shown; no mastery evidence is written, because the questions come from the lesson the learner has done.
 * Sending no answers skips the quick check and returns the review to practice all the same.
 */
@Service
public class SubmitTheoryCheckUseCase {
    private final LearningContentClient content;
    private final LearnerLock lock;
    private final ReviewItemRepository reviews;
    private final ReviewTheoryCheckLog checks;
    private final TransactionTemplate transaction;
    private final TheoryFocus theory = new TheoryFocus();
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public SubmitTheoryCheckUseCase(LearningContentClient content, LearnerLock lock, ReviewItemRepository reviews,
                                    ReviewTheoryCheckLog checks, PlatformTransactionManager transactionManager) {
        this.content = content;
        this.lock = lock;
        this.reviews = reviews;
        this.checks = checks;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public TheoryCheckResult execute(UUID userId, UUID reviewId, SubmitExerciseCommand command) {
        ReviewItem owned = reviews.findOwned(userId, reviewId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
        var saved = checks.find(command.requestId());
        if (saved.isPresent()) return replay(userId, reviewId, saved.get());
        if (owned.status() != ReviewStatus.PENDING || owned.stage() != ReviewStage.THEORY) {
            throw new LearningRequestException(409, "THEORY_NOT_REQUIRED", "This review is not at the theory stage");
        }
        List<Question> questions;
        try {
            Lesson lesson = content.getLesson(owned.lessonId());
            questions = theory.of(lesson, owned.knowledgePointId()).quickCheck();
        } catch (LearningRequestException error) {
            if (error.getStatus() != 404 || !"NOT_FOUND".equals(error.getCode())) throw error;
            questions = List.of();
        }
        List<Question> preparedQuestions = questions;
        return transaction.execute(status -> {
            lock.lock(userId);
            ReviewItem review = reviews.findOwned(userId, reviewId)
                    .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
            var replay = checks.find(command.requestId());
            if (replay.isPresent()) return replay(userId, reviewId, replay.get());
            if (review.status() != ReviewStatus.PENDING || review.stage() != ReviewStage.THEORY) {
                throw new LearningRequestException(409, "THEORY_NOT_REQUIRED", "This review is not at the theory stage");
            }
            return complete(userId, reviewId, review, command, preparedQuestions);
        });
    }

    private TheoryCheckResult complete(UUID userId, UUID reviewId, ReviewItem review, SubmitExerciseCommand command,
                                       List<Question> questions) {
        List<UUID> ids = questions.stream().map(Question::questionVersionId).toList();
        List<SubmissionResult.AnswerResult> results = new ArrayList<>();
        int correct = 0;
        if (questions.isEmpty()) {
            if (!command.answers().isEmpty()) {
                throw new LearningRequestException(422, "INVALID_ANSWERS", "This review has no quick-check questions");
            }
        } else if (command.answers().isEmpty()) {
            // The quick check is optional: a learner who goes on to the practice set without answering it has read the
            // theory, which is all this step asks. Nothing is graded and no result is shown.
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
        TheoryCheckResult response = new TheoryCheckResult(reviewId, correct, results.size(), List.copyOf(results),
                review.stage().name());
        checks.save(userId, reviewId, ids, command, response);
        return response;
    }

    private static TheoryCheckResult replay(UUID userId, UUID reviewId, ReviewTheoryCheckLog.Stored saved) {
        if (!saved.userId().equals(userId) || !saved.reviewId().equals(reviewId)) {
            throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
        }
        return saved.response();
    }
}
