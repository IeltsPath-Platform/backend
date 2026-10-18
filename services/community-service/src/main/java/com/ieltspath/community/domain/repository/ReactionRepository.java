package com.ieltspath.community.domain.repository;

import com.ieltspath.community.domain.vo.ReactionType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ReactionRepository {
    boolean add(UUID postId, UUID userId, ReactionType type);

    boolean remove(UUID postId, UUID userId, ReactionType type);

    Map<ReactionType, Long> counts(UUID postId);

    Map<UUID, Map<ReactionType, Long>> counts(Set<UUID> postIds);
}
