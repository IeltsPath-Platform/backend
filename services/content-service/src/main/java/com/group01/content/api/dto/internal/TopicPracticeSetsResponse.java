package com.group01.content.api.dto.internal;

import com.group01.content.application.result.LessonPracticeSetsResult;

import java.util.List;
import java.util.UUID;

/** Every published lesson of a topic in order, each with its practice sets. */
public record TopicPracticeSetsResponse(List<LessonPracticeSets> lessons) {
    public record LessonPracticeSets(UUID lessonId, List<LessonPracticeSetResponse> practiceSets) {}

    public static TopicPracticeSetsResponse from(List<LessonPracticeSetsResult> lessons) {
        return new TopicPracticeSetsResponse(lessons.stream()
                .map(lesson -> new LessonPracticeSets(lesson.lessonId(),
                        lesson.practiceSets().stream().map(LessonPracticeSetResponse::from).toList()))
                .toList());
    }
}
