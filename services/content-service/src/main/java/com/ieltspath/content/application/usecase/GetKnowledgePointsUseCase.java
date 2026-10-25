package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetKnowledgePointsUseCase {

    private final KnowledgePointRepository knowledgePointRepository;
    private final ErrorTypeRepository errorTypeRepository;

    public GetKnowledgePointsUseCase(KnowledgePointRepository knowledgePointRepository,
                                    ErrorTypeRepository errorTypeRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.errorTypeRepository = errorTypeRepository;
    }

    public List<KnowledgePointResult> execute(UUID topicId) {
        List<KnowledgePoint> points = topicId != null
                ? knowledgePointRepository.findByTopicId(topicId)
                : knowledgePointRepository.findAll();
        if (points.isEmpty()) {
            return List.of();
        }
        var foundTrapIds = errorTypeRepository.findTrapKnowledgePointIds(
                points.stream().map(KnowledgePoint::getId).toList());
        var trapIds = foundTrapIds == null ? java.util.Set.<UUID>of() : foundTrapIds;
        return points.stream().filter(point -> !trapIds.contains(point.getId())).map(KnowledgePointResult::from).toList();
    }
}
