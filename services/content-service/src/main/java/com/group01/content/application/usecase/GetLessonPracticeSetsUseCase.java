package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.LessonPracticeSetResult;
import com.group01.content.domain.exception.LessonNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** The practice sets that make up a published lesson's Practice. */
@Service
@Transactional(readOnly = true)
public class GetLessonPracticeSetsUseCase {
    private final LearningContentReader reader;

    public GetLessonPracticeSetsUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public List<LessonPracticeSetResult> execute(UUID lessonId) {
        if (!reader.publishedLessonExists(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        return reader.lessonPracticeSets(lessonId);
    }
}
