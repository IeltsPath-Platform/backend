package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.KnowledgePoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgePointRepository {
    KnowledgePoint save(KnowledgePoint kp);
    Optional<KnowledgePoint> findById(UUID id);
    List<KnowledgePoint> findByIds(Collection<UUID> ids);
    Optional<KnowledgePoint> findByCode(String code);
    List<KnowledgePoint> findByTopicId(UUID topicId);
    List<KnowledgePoint> findAll();
    boolean existsByCode(String code);
    boolean isInUse(UUID id);
}

