package com.ieltspath.community.domain.repository;

import com.ieltspath.community.domain.vo.CommentInteractions;
import com.ieltspath.community.domain.vo.ReactionType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface CommentReactionRepository {
    boolean add(UUID commentId, UUID userId, ReactionType type);
    boolean remove(UUID commentId, UUID userId, ReactionType type);
    Map<UUID, Map<ReactionType, Long>> counts(Set<UUID> commentIds);
    Map<UUID, ReactionType> mine(Set<UUID> commentIds, UUID viewer);
    Map<UUID, CommentInteractions> interactions(Set<UUID> commentIds, UUID viewer);
}
