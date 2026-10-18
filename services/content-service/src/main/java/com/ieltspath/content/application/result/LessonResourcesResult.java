package com.ieltspath.content.application.result;

import java.util.List;
import java.util.UUID;

/** A lesson's Markdown summary ({@code null} when it has none) and its resources by type, then order. */
public record LessonResourcesResult(UUID lessonId, String summaryMarkdown, List<LessonResourceResult> resources) {}
