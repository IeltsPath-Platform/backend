package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.LessonResourceResult;
import com.ieltspath.content.application.result.LessonResourcesResult;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** A lesson's summary and resources: any lesson for authors, only published lessons for the learning flow. */
@Service
@Transactional(readOnly = true)
public class ListLessonResourcesUseCase {
    private final LessonResourceRepository resources;
    private final LearningContentReader lessons;

    public ListLessonResourcesUseCase(LessonResourceRepository resources, LearningContentReader lessons) {
        this.resources = resources;
        this.lessons = lessons;
    }

    public LessonResourcesResult execute(UUID lessonId) {
        if (!lessons.lessonExists(lessonId)) throw new LessonNotFoundException(lessonId);
        return load(lessonId);
    }

    public LessonResourcesResult executePublished(UUID lessonId) {
        if (!lessons.publishedLessonExists(lessonId)) throw new LessonNotFoundException(lessonId);
        return load(lessonId);
    }

    private LessonResourcesResult load(UUID lessonId) {
        return new LessonResourcesResult(lessonId, resources.findSummaryMarkdown(lessonId),
                resources.findByLessonId(lessonId).stream().map(LessonResourceResult::from).toList());
    }
}
