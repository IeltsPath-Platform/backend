package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.exception.DuplicateImportRefException;
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
        String importRef = Question.normalizeImportRef(command.importRef());
        if (importRef != null && questionRepository.findByImportRef(importRef).isPresent()) {
            throw new DuplicateImportRefException(importRef);
        }

        Question question = Question.create(
                command.questionType(),
                command.skill(),
                command.requiredFeatureKey(),
                command.purpose(),
                importRef
        );

        return QuestionResult.of(questionRepository.save(question));
    }
}
