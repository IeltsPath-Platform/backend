package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.KnowledgeTreeResult;

import java.util.List;

public record KnowledgeTreeResponse(List<KnowledgeTreeNodeResponse> nodes, List<KnowledgeTreeEdgeResponse> edges) {
    public static KnowledgeTreeResponse from(KnowledgeTreeResult result) {
        return new KnowledgeTreeResponse(
                result.nodes().stream().map(KnowledgeTreeNodeResponse::from).toList(),
                result.edges().stream().map(edge -> new KnowledgeTreeEdgeResponse(edge.from(), edge.to())).toList());
    }
}
