package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateKnowledgePointCommand;
import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.exception.InvalidKnowledgePointCodeException;
import com.ieltspath.content.domain.exception.TopicNotFoundException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.KnowledgePointCodeRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateKnowledgePointUseCase {

    private final KnowledgePointRepository knowledgePointRepository;
    private final TopicRepository topicRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public CreateKnowledgePointUseCase(KnowledgePointRepository knowledgePointRepository,
                                       TopicRepository topicRepository, CourseRepository courseRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.topicRepository = topicRepository;
        this.courseRepository = courseRepository;
    }

    public CreateKnowledgePointUseCase(KnowledgePointRepository knowledgePointRepository,
                                       TopicRepository topicRepository) {
        this(knowledgePointRepository, topicRepository, null);
    }

    public KnowledgePointResult execute(CreateKnowledgePointCommand command) {
        if (!KnowledgePointCodeRules.isValid(command.code(), command.kind(), command.skill())) {
            throw new InvalidKnowledgePointCodeException(command.code());
        }
        if (knowledgePointRepository.existsByCode(command.code())) {
            throw new DuplicateCodeException("KnowledgePoint", command.code());
        }
        var topic = topicRepository.findById(command.topicId())
                .orElseThrow(() -> new TopicNotFoundException(command.topicId()));

        CefrLevel cefrLevel = command.cefrLevel();
        if (cefrLevel == null && courseRepository != null && topic.getCourseId() != null) {
            cefrLevel = courseRepository.findById(topic.getCourseId())
                    .map(course -> CefrLevel.forBand(course.getBandLevel()))
                    .orElse(null);
        }

        KnowledgePoint kp = KnowledgePoint.create(
                command.topicId(),
                command.code(),
                command.name(),
                command.kind(),
                command.learningType(),
                command.skill(),
                command.description(),
                cefrLevel,
                command.canDo(),
                command.scopeIn(),
                command.scopeOut()
        );
        return KnowledgePointResult.from(knowledgePointRepository.save(kp));
    }
}
