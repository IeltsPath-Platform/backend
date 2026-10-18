package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.UpdateKnowledgePointCommand;
import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.exception.InvalidKnowledgePointCodeException;
import com.ieltspath.content.domain.exception.KnowledgePointInUseException;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.exception.TopicNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.vo.KnowledgePointCodeRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class UpdateKnowledgePointUseCase {
    private final KnowledgePointRepository knowledgePointRepository;
    private final TopicRepository topicRepository;

    public UpdateKnowledgePointUseCase(KnowledgePointRepository knowledgePointRepository,
                                       TopicRepository topicRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.topicRepository = topicRepository;
    }

    public KnowledgePointResult execute(java.util.UUID id, UpdateKnowledgePointCommand command) {
        KnowledgePoint knowledgePoint = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new KnowledgePointNotFoundException(id));
        if (topicRepository.findById(command.topicId()).isEmpty()) {
            throw new TopicNotFoundException(command.topicId());
        }
        if (!knowledgePoint.getCode().equals(command.code())) {
            if (!KnowledgePointCodeRules.isValid(command.code(), command.kind(), command.skill())) {
                throw new InvalidKnowledgePointCodeException(command.code());
            }
            knowledgePointRepository.findByCode(command.code())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> { throw new DuplicateCodeException("KnowledgePoint", command.code()); });
        }
        if (!KnowledgePointCodeRules.isPrefixCompatible(command.code(), command.kind(), command.skill())) {
            throw new InvalidKnowledgePointCodeException(command.code());
        }
        if (!knowledgePoint.getTopicId().equals(command.topicId()) && knowledgePointRepository.isInUse(id)) {
            throw new KnowledgePointInUseException(id);
        }

        knowledgePoint.update(command.topicId(), command.code(), command.name(), command.kind(),
                command.learningType(), command.skill(), command.description(), command.cefrLevel(), command.canDo(),
                command.scopeIn(), command.scopeOut());
        return KnowledgePointResult.from(knowledgePointRepository.save(knowledgePoint));
    }
}
