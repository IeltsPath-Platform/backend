package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetQuestionDifficultiesUseCase {
    private final LearningContentReader learningContentReader;

    public GetQuestionDifficultiesUseCase(LearningContentReader learningContentReader) {
        this.learningContentReader = learningContentReader;
    }

    public Map<UUID, QuestionDifficulty> execute(Collection<UUID> questionVersionIds) {
        return learningContentReader.questionDifficulties(questionVersionIds);
    }
}
