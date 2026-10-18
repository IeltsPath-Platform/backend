package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.InvalidPrerequisiteException;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.KnowledgePrerequisiteRepository;
import com.ieltspath.content.domain.service.KnowledgeGraph;
import com.ieltspath.content.domain.vo.ContentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ReplaceKnowledgePointPrerequisitesUseCase {
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgePrerequisiteRepository prerequisiteRepository;
    private final KnowledgeGraph knowledgeGraph = new KnowledgeGraph();

    public ReplaceKnowledgePointPrerequisitesUseCase(KnowledgePointRepository knowledgePointRepository,
                                                     KnowledgePrerequisiteRepository prerequisiteRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    public List<KnowledgePointResult> execute(UUID id, List<UUID> requestedIds) {
        prerequisiteRepository.lockTree();
        knowledgePointRepository.findById(id).orElseThrow(() -> new KnowledgePointNotFoundException(id));

        List<UUID> prerequisiteIds = List.copyOf(new LinkedHashSet<>(requestedIds));
        Map<UUID, KnowledgePoint> prerequisitesById = new HashMap<>();
        knowledgePointRepository.findByIds(prerequisiteIds)
                .forEach(prerequisite -> prerequisitesById.put(prerequisite.getId(), prerequisite));
        List<UUID> invalidIds = new ArrayList<>();
        for (UUID prerequisiteId : prerequisiteIds) {
            var prerequisite = prerequisitesById.get(prerequisiteId);
            if (id.equals(prerequisiteId) || prerequisite == null
                    || prerequisite.getStatus() != ContentStatus.ACTIVE) {
                invalidIds.add(prerequisiteId);
            }
        }
        if (!invalidIds.isEmpty()) {
            throw new InvalidPrerequisiteException(invalidIds);
        }

        List<KnowledgeGraph.Edge> candidateEdges = new ArrayList<>();
        for (KnowledgePrerequisiteRepository.Edge edge : prerequisiteRepository.findAll()) {
            if (!edge.knowledgePointId().equals(id)) {
                candidateEdges.add(new KnowledgeGraph.Edge(edge.prerequisiteId(), edge.knowledgePointId()));
            }
        }
        prerequisiteIds.forEach(prerequisiteId -> candidateEdges.add(new KnowledgeGraph.Edge(prerequisiteId, id)));
        knowledgeGraph.ensureAcyclic(candidateEdges);
        prerequisiteRepository.replace(id, prerequisiteIds);
        return prerequisiteIds.stream()
                .map(prerequisitesById::get)
                .map(KnowledgePointResult::from)
                .toList();
    }
}
