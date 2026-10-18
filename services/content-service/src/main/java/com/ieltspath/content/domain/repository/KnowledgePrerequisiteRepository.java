package com.ieltspath.content.domain.repository;

import java.util.List;
import java.util.UUID;

public interface KnowledgePrerequisiteRepository {
    record Edge(UUID prerequisiteId, UUID knowledgePointId) {}

    void lockTree();

    List<UUID> findPrerequisiteIds(UUID knowledgePointId);

    List<Edge> findAll();

    void replace(UUID knowledgePointId, List<UUID> prerequisiteIds);
}
