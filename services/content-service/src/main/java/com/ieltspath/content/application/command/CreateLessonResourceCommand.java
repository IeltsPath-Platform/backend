package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.LessonResourceType;

import java.util.UUID;

public record CreateLessonResourceCommand(UUID lessonId, LessonResourceType resourceType, String title, String url,
                                          int sortOrder) {}
