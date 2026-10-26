package com.ieltspath.library.application.result;

import java.util.List;
import java.util.UUID;

/** A bounded graph: topic to words, and words to their collocations and synonyms. */
public record VocabularyMindmapResult(List<Node> nodes, List<Edge> edges) {
    /** {@code itemId} is set on word nodes so the page can open the word. */
    public record Node(UUID id, String type, String label, UUID itemId) {
    }

    public record Edge(UUID from, UUID to, String kind) {
    }
}
