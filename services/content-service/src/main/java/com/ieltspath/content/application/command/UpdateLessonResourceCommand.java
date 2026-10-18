package com.ieltspath.content.application.command;

import java.util.UUID;

public record UpdateLessonResourceCommand(UUID lessonId, UUID resourceId, String title, String url, int sortOrder) {}
