package com.ieltspath.community.domain.vo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record CommentInteractions(Map<ReactionType, Long> reactionCounts, ReactionType myReaction) {
    public CommentInteractions {
        var counts = new EnumMap<ReactionType, Long>(ReactionType.class);
        for (var type : ReactionType.values()) {
            counts.put(type, reactionCounts.getOrDefault(type, 0L));
        }
        reactionCounts = Collections.unmodifiableMap(counts);
    }

    public static CommentInteractions empty() {
        return new CommentInteractions(Map.of(), null);
    }
}
