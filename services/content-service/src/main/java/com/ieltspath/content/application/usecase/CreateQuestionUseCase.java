package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateQuestionUseCase {

    private final QuestionRepository questionRepository;

    public CreateQuestionUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public QuestionResult execute(CreateQuestionCommand command) {
        Question question = Question.create(
                command.questionType(),
                command.skill(),
                command.requiredFeatureKey(),
                command.purpose()
        );

        Question saved = questionRepository.save(question);
        return new QuestionResult(
                saved.getId(),
                saved.getQuestionType(),
                saved.getSkill(),
                saved.getRequiredFeatureKey(),
                saved.getStatus(),
                saved.getCurrentPublishedVersionId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saved.getPurpose()
        );
    }
}
