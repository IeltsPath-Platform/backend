package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.domain.exception.LessonNotFoundException;
import com.group01.content.domain.vo.BlockType;
import com.group01.content.domain.vo.LessonBlockKind;
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
        LessonContentResult lesson = reader.publishedLesson(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
        return lesson.withBlocks(lesson.blocks().stream().map(GetLessonContentUseCase::classified).toList());
    }

    private static LessonContentResult.Block classified(LessonContentResult.Block block) {
        if (block.blockType() != BlockType.EXERCISE) {
            return block;
        }
        return block.withBlockKind(LessonBlockKind.classify(block.questions().stream()
                .map(q -> new LessonBlockKind.QuestionSpec(q.specType(), q.specPassBand()))
                .toList()));
    }
}
