package com.group01.content.domain.repository;

import com.group01.content.domain.aggregate.KnowledgePoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgePointRepository {
    KnowledgePoint save(KnowledgePoint kp);
    Optional<KnowledgePoint> findById(UUID id);
    Optional<KnowledgePoint> findByCode(String code);
    List<KnowledgePoint> findByTopicId(UUID topicId);
    List<KnowledgePoint> findAll();
    boolean existsByCode(String code);
}

