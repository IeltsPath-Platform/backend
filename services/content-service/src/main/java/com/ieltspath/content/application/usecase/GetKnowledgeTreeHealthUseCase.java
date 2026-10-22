package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.KnowledgeTreeHealthResult;
import com.ieltspath.content.application.result.KnowledgeTreeNodeResult;
import com.ieltspath.content.domain.repository.KnowledgePrerequisiteRepository;
import com.ieltspath.content.domain.service.KnowledgeGraph;
import com.ieltspath.content.domain.vo.CefrLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Checks the active knowledge graph: points with no edge, points without CEFR level or can-do, and CEFR-inverted edges. */
@Service
@Transactional(readOnly = true)
public class GetKnowledgeTreeHealthUseCase {
    private final LearningContentReader learningContentReader;
    private final KnowledgePrerequisiteRepository prerequisiteRepository;
    private final KnowledgeGraph knowledgeGraph = new KnowledgeGraph();

    public GetKnowledgeTreeHealthUseCase(LearningContentReader learningContentReader,
                                         KnowledgePrerequisiteRepository prerequisiteRepository) {
        this.learningContentReader = learningContentReader;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    public KnowledgeTreeHealthResult execute() {
        List<KnowledgeTreeNodeResult> nodes = learningContentReader.knowledgeTreeNodes(null, null, false);
        Set<UUID> nodeIds = new HashSet<>();
        Map<UUID, CefrLevel> levels = new HashMap<>();
        List<UUID> missingMetadataIds = nodes.stream()
                .peek(node -> {
                    nodeIds.add(node.id());
                    levels.put(node.id(), node.cefrLevel());
                })
                .filter(node -> node.cefrLevel() == null || node.canDo() == null || node.canDo().isBlank())
                .map(KnowledgeTreeNodeResult::id)
                .toList();
        List<KnowledgeGraph.Edge> edges = prerequisiteRepository.findAll().stream()
                .filter(edge -> nodeIds.contains(edge.knowledgePointId()) && nodeIds.contains(edge.prerequisiteId()))
                .map(edge -> new KnowledgeGraph.Edge(edge.prerequisiteId(), edge.knowledgePointId()))
                .toList();
        return new KnowledgeTreeHealthResult(
                List.copyOf(knowledgeGraph.isolated(nodeIds, edges)),
                missingMetadataIds,
                knowledgeGraph.levelViolations(edges, levels).stream()
                        .map(edge -> new KnowledgeTreeHealthResult.LevelViolation(edge.from(), edge.to()))
                        .toList());
    }
}
