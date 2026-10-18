package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import com.ieltspath.content.domain.vo.LessonSummaryMarkdown;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Replaces a lesson's Markdown summary; blank text removes it. */
@Service
@Transactional
public class UpdateLessonSummaryUseCase {
    private final LessonResourceRepository resources;
    private final LearningContentReader lessons;

    public UpdateLessonSummaryUseCase(LessonResourceRepository resources, LearningContentReader lessons) {
        this.resources = resources;
        this.lessons = lessons;
    }

    public void execute(UUID lessonId, String markdown) {
        String summary = LessonSummaryMarkdown.normalize(markdown);
        if (!lessons.lessonExists(lessonId)) throw new LessonNotFoundException(lessonId);
        resources.updateSummaryMarkdown(lessonId, summary);
    }
}
