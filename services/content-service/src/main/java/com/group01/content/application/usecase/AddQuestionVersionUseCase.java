package com.group01.content.application.usecase;

import com.group01.content.application.command.AddQuestionVersionCommand;
import com.group01.content.application.result.QuestionResult;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.entity.QuestionVersion;
import com.group01.content.domain.exception.QuestionNotFoundException;
import com.group01.content.domain.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddQuestionVersionUseCase {

    private final QuestionRepository questionRepository;

    public AddQuestionVersionUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public QuestionResult execute(AddQuestionVersionCommand command) {
        Question question = questionRepository.findById(command.questionId())
                .orElseThrow(() -> new QuestionNotFoundException(command.questionId()));

        QuestionVersion version = QuestionVersion.create(
                command.questionId(),
                command.versionNumber(),
                command.stem(),
                command.options(),
                command.answerSpecJson(),
                command.explanation(),
                command.difficulty()
        );

        if (command.knowledgePoints() != null) {
            for (var kp : command.knowledgePoints()) {
                version.addKnowledgePoint(kp);
            }
        }

        question.addVersion(version);
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

