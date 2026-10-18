package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.KnowledgeTreeResult;
import com.ieltspath.content.domain.repository.KnowledgePrerequisiteRepository;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetKnowledgeTreeUseCase {
    private final LearningContentReader learningContentReader;
    private final KnowledgePrerequisiteRepository prerequisiteRepository;

    public GetKnowledgeTreeUseCase(LearningContentReader learningContentReader,
                                   KnowledgePrerequisiteRepository prerequisiteRepository) {
        this.learningContentReader = learningContentReader;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    public KnowledgeTreeResult execute(UUID courseId, Skill skill, boolean includeInactive) {
        var nodes = learningContentReader.knowledgeTreeNodes(courseId, skill, includeInactive);
        Set<UUID> nodeIds = new HashSet<>(nodes.stream().map(node -> node.id()).toList());
        List<KnowledgeTreeResult.Edge> edges = prerequisiteRepository.findAll().stream()
                .filter(edge -> nodeIds.contains(edge.knowledgePointId()) && nodeIds.contains(edge.prerequisiteId()))
                .map(edge -> new KnowledgeTreeResult.Edge(edge.prerequisiteId(), edge.knowledgePointId()))
                .toList();
        return new KnowledgeTreeResult(nodes, edges);
    }
}
