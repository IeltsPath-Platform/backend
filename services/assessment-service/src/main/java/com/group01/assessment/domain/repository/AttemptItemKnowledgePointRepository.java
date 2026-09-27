package com.group01.assessment.domain.repository;

import com.group01.assessment.domain.entity.AttemptItemKnowledgePoint;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AttemptItemKnowledgePointRepository {
    List<AttemptItemKnowledgePoint> saveAll(List<AttemptItemKnowledgePoint> values);

    List<AttemptItemKnowledgePoint> findByAttemptItemIds(Collection<UUID> attemptItemIds);
}
