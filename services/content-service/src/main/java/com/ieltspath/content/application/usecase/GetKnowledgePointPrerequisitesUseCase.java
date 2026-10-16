package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.KnowledgePrerequisiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetKnowledgePointPrerequisitesUseCase {
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgePrerequisiteRepository prerequisiteRepository;

    public GetKnowledgePointPrerequisitesUseCase(KnowledgePointRepository knowledgePointRepository,
                                                 KnowledgePrerequisiteRepository prerequisiteRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    public List<KnowledgePointResult> execute(UUID id) {
        if (knowledgePointRepository.findById(id).isEmpty()) {
            throw new KnowledgePointNotFoundException(id);
        }
        List<UUID> prerequisiteIds = prerequisiteRepository.findPrerequisiteIds(id);
        Map<UUID, KnowledgePoint> prerequisitesById = new HashMap<>();
        knowledgePointRepository.findByIds(prerequisiteIds)
                .forEach(prerequisite -> prerequisitesById.put(prerequisite.getId(), prerequisite));
        return prerequisiteIds.stream()
                .map(prerequisiteId -> {
                    var prerequisite = prerequisitesById.get(prerequisiteId);
                    if (prerequisite == null) {
                        throw new KnowledgePointNotFoundException(prerequisiteId);
                    }
                    return prerequisite;
                })
                .map(KnowledgePointResult::from)
                .toList();
    }
}
