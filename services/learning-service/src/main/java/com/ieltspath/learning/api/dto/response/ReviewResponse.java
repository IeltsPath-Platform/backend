package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieltspath.learning.application.result.LessonResult;
import com.ieltspath.learning.application.result.ReviewResult;
import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.List;
import java.util.UUID;

/**
 * Review set and quick-check questions use the lesson allowlist plus {@code hint}; no answer spec, explanation or
 * transcript. {@code set} is null at the theory stage, {@code quickCheck} is empty at the practice stage.
 */
public record ReviewResponse(UUID reviewId, String reviewStatus, UUID lessonId, List<String> theory, ReviewSet set,
                             UUID knowledgePointId, LearningSkill skill, String stage, String theoryReason,
                             String theoryScope, int failedSets, int maxFailedSets,
                             List<LessonResponse.Question> quickCheck) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReviewSet(UUID reviewSetId, UUID packageId, UUID packageVersionId, String passage, Audio audio,
                            List<LessonResponse.Question> questions) {}

    public record Audio(String mediaUrl, Integer durationSeconds) {}

    public static ReviewResponse from(ReviewResult result) {
        ReviewResult.ReviewSet set = result.set();
        return new ReviewResponse(result.reviewId(), result.reviewStatus(), result.lessonId(), result.theory(),
                set == null ? null : new ReviewSet(set.reviewSetId(), set.packageId(), set.packageVersionId(),
                        set.passage(),
                        set.audio() == null ? null : new Audio(set.audio().mediaUrl(), set.audio().durationSeconds()),
                        questions(set.questions())),
                result.knowledgePointId(), result.skill(), result.stage(), result.theoryReason(), result.theoryScope(),
                result.failedSets(), result.maxFailedSets(), questions(result.quickCheck()));
    }

    private static List<LessonResponse.Question> questions(List<LessonResult.Question> questions) {
        return questions.stream().map(question -> new LessonResponse.Question(question.questionVersionId(),
                question.sortOrder(), question.stem(), question.options() == null ? null : question.options().stream()
                .map(option -> new LessonResponse.Option(option.optionKey(), option.content(), option.sortOrder()))
                .toList(), question.hint())).toList();
    }
}
