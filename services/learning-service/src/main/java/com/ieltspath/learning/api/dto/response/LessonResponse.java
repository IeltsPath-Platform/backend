package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieltspath.learning.application.result.LessonResult;
import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.List;
import java.util.UUID;

/** A lesson and its status; its summary, slides and documents are read from {@code /lessons/{id}/resources}. */
public record LessonResponse(UUID lessonId, UUID topicId, String code, String title, String summary,
                             int sortOrder, String status, LearningSkill skill, List<LearningSkill> skills) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options, String hint) {
        public Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options) {
            this(questionVersionId, sortOrder, stem, options, null);
        }
    }

    public record Option(String optionKey, String content, int sortOrder) {}

    public static LessonResponse from(LessonResult result) {
        return new LessonResponse(result.lessonId(), result.topicId(), result.code(), result.title(), result.summary(),
                result.sortOrder(), result.status(), result.skill(), result.skills().stream().sorted().toList());
    }
}
