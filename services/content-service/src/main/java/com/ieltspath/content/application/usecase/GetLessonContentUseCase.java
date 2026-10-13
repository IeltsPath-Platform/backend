package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.LessonContentResult;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetLessonContentUseCase {
    private final LearningContentReader reader;

    public GetLessonContentUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public LessonContentResult execute(UUID lessonId) {
        return reader.publishedLesson(lessonId).orElseThrow(() -> new LessonNotFoundException(lessonId));
    }
}
