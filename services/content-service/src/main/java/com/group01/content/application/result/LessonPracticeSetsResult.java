package com.group01.content.application.result;

import java.util.List;
import java.util.UUID;

/** The practice sets of one lesson of a topic; empty when the lesson has none. */
public record LessonPracticeSetsResult(UUID lessonId, List<LessonPracticeSetResult> practiceSets) {}
