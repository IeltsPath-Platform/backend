package com.group01.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group01.learning.application.result.ReviewResult;

import java.util.List;
import java.util.UUID;

/** Review set questions use the lesson allowlist; no answer spec, explanation or transcript. */
public record ReviewResponse(UUID reviewId, String reviewStatus, UUID lessonId, List<String> theory, ReviewSet set) {
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
                        set.questions().stream().map(question -> new LessonResponse.Question(
                                question.questionVersionId(), question.sortOrder(), question.stem(),
                                question.options() == null ? null : question.options().stream()
                                        .map(option -> new LessonResponse.Option(option.optionKey(), option.content(),
                                                option.sortOrder())).toList())).toList()));
    }
}
