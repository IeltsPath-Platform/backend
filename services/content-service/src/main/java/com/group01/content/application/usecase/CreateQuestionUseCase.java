package com.group01.content.application.usecase;

import com.group01.content.application.command.CreateQuestionCommand;
import com.group01.content.application.result.QuestionResult;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.repository.QuestionRepository;
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
                command.accessLevel()
        );

        Question saved = questionRepository.save(question);
        return new QuestionResult(
                saved.getId(),
                saved.getQuestionType(),
                saved.getSkill(),
                saved.getAccessLevel(),
                saved.getStatus(),
                saved.getCurrentPublishedVersionId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}

