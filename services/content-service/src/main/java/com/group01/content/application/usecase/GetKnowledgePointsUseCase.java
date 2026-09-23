package com.group01.content.application.usecase;

import com.group01.content.application.result.KnowledgePointResult;
import com.group01.content.domain.aggregate.KnowledgePoint;
import com.group01.content.domain.repository.KnowledgePointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetKnowledgePointsUseCase {

    private final KnowledgePointRepository knowledgePointRepository;

    public GetKnowledgePointsUseCase(KnowledgePointRepository knowledgePointRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
    }

    public List<KnowledgePointResult> execute(UUID topicId) {
        List<KnowledgePoint> points;
        if (topicId != null) {
            points = knowledgePointRepository.findByTopicId(topicId);
        } else {
            points = knowledgePointRepository.findAll();
        }

        return points.stream()
                .map(kp -> new KnowledgePointResult(
                        kp.getId(),
                        kp.getTopicId(),
                        kp.getCode(),
                        kp.getName(),
                        kp.getKind(),
                        kp.getSkill(),
                        kp.getDescription(),
                        kp.getStatus(),
                        kp.getCreatedAt(),
                        kp.getUpdatedAt()
                ))
                .toList();
    }
}

