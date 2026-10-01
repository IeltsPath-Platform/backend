package com.group01.content.application.usecase;

import com.group01.content.application.command.CreateKnowledgePointCommand;
import com.group01.content.application.result.KnowledgePointResult;
import com.group01.content.domain.aggregate.KnowledgePoint;
import com.group01.content.domain.exception.DuplicateCodeException;
import com.group01.content.domain.exception.TopicNotFoundException;
import com.group01.content.domain.repository.KnowledgePointRepository;
import com.group01.content.domain.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateKnowledgePointUseCase {

    private final KnowledgePointRepository knowledgePointRepository;
    private final TopicRepository topicRepository;

    public CreateKnowledgePointUseCase(KnowledgePointRepository knowledgePointRepository,
                                       TopicRepository topicRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.topicRepository = topicRepository;
    }

    public KnowledgePointResult execute(CreateKnowledgePointCommand command) {
        if (knowledgePointRepository.existsByCode(command.code())) {
            throw new DuplicateCodeException("KnowledgePoint", command.code());
        }
        if (topicRepository.findById(command.topicId()).isEmpty()) {
            throw new TopicNotFoundException(command.topicId());
        }

        KnowledgePoint kp = KnowledgePoint.create(
                command.topicId(),
                command.code(),
                command.name(),
                command.kind(),
                command.learningType(),
                command.skill(),
                command.description()
        );
        return KnowledgePointResult.from(knowledgePointRepository.save(kp));
    }
}
