package com.ieltspath.content.application.result;

import java.util.List;
import java.util.UUID;

public record KnowledgeTreeResult(List<KnowledgeTreeNodeResult> nodes, List<Edge> edges) {
    public record Edge(UUID from, UUID to) {}
}
