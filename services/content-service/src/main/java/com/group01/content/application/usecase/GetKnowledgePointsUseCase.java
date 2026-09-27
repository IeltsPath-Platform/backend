package com.group01.content.application.usecase;

import com.group01.content.application.result.KnowledgePointResult;
import com.group01.content.domain.aggregate.KnowledgePoint;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.repository.KnowledgePointRepository;
import com.group01.content.domain.repository.TopicRepository;
import com.group01.content.domain.vo.BandRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetKnowledgePointsUseCase {

    private final KnowledgePointRepository knowledgePointRepository;
    private final TopicRepository topicRepository;

    public GetKnowledgePointsUseCase(KnowledgePointRepository knowledgePointRepository,
                                     TopicRepository topicRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.topicRepository = topicRepository;
    }

    public List<KnowledgePointResult> execute(UUID topicId) {
        List<KnowledgePoint> points;
        if (topicId != null) {
            points = knowledgePointRepository.findByTopicId(topicId);
        } else {
            points = knowledgePointRepository.findAll();
        }

        // One batched lookup for the topics whose band the points may inherit.
        Set<UUID> topicIds = points.stream().map(KnowledgePoint::getTopicId).collect(Collectors.toSet());
        Map<UUID, BandRange> topicBands = topicIds.isEmpty() ? Map.of() : topicRepository.findAllByIds(topicIds)
                .stream()
                .collect(Collectors.toMap(Topic::getId, Topic::getBand));

        return points.stream()
                .map(kp -> new KnowledgePointResult(
                        kp.getId(),
                        kp.getTopicId(),
                        kp.getCode(),
                        kp.getName(),
                        kp.getKind(),
                        kp.getLearningType(),
                        kp.getSkill(),
                        kp.getDescription(),
                        kp.getStatus(),
                        kp.getCreatedAt(),
                        kp.getUpdatedAt(),
                        kp.getBand(),
                        kp.getBand().orInherit(topicBands.getOrDefault(kp.getTopicId(), BandRange.UNBOUNDED))
                ))
                .toList();
    }
}
