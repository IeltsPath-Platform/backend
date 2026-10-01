package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.domain.exception.LessonNotFoundException;
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
