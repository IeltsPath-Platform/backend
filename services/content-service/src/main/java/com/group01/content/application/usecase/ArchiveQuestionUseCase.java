package com.group01.content.application.usecase;

import com.group01.content.application.result.QuestionResult;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.exception.QuestionNotFoundException;
import com.group01.content.domain.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ArchiveQuestionUseCase {

    private final QuestionRepository questionRepository;

    public ArchiveQuestionUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public QuestionResult execute(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        question.archive();
        Question saved = questionRepository.save(question);

        return new QuestionResult(
                saved.getId(),
                saved.getQuestionType(),
                saved.getSkill(),
                saved.getRequiredFeatureKey(),
                saved.getStatus(),
                saved.getCurrentPublishedVersionId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
