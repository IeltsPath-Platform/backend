package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.LessonPracticeSetResult;
import com.group01.content.domain.exception.LessonNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import com.group01.content.domain.vo.Skill;

/** The practice sets that make up a published lesson's Practice. */
@Service
@Transactional(readOnly = true)
public class GetLessonPracticeSetsUseCase {
    private final LearningContentReader reader;

    public GetLessonPracticeSetsUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public List<LessonPracticeSetResult> execute(UUID lessonId) {
        return execute(lessonId, Optional.empty());
    }

    public List<LessonPracticeSetResult> execute(UUID lessonId, Optional<Skill> skill) {
        if (!reader.publishedLessonExists(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        if (skill.orElse(null) == Skill.ALL) throw new IllegalArgumentException("skill must select one skill");
        return skill.isEmpty() ? reader.lessonPracticeSets(lessonId) : reader.lessonPracticeSets(lessonId, skill);
    }
}
