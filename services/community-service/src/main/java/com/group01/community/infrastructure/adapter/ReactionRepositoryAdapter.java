package com.group01.community.infrastructure.adapter;

import com.group01.community.domain.repository.ReactionRepository;
import com.group01.community.domain.vo.ReactionType;
import com.group01.community.infrastructure.persistence.entity.PostReactionId;
import com.group01.community.infrastructure.persistence.repository.PostReactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class ReactionRepositoryAdapter implements ReactionRepository {
    private final PostReactionJpaRepository repository;

    public void add(UUID postId, UUID userId, ReactionType type) {
        repository.insertIgnore(postId, userId, type.name());
    }

    public void remove(UUID postId, UUID userId, ReactionType type) {
        repository.deleteById(new PostReactionId(postId, userId, type));
    }

    public Map<ReactionType, Long> counts(UUID postId) {
        Map<ReactionType, Long> result = new EnumMap<>(ReactionType.class);
        repository.countByPost(postId).forEach(row -> result.put((ReactionType) row[0], (Long) row[1]));
        return result;
    }

    public Map<UUID, Map<ReactionType, Long>> counts(Set<UUID> postIds) {
        Map<UUID, Map<ReactionType, Long>> result = new HashMap<>();
        if (postIds.isEmpty()) {
            return result;
        }
        repository.countByPosts(postIds).forEach(row -> result
                .computeIfAbsent((UUID) row[0], ignored -> new EnumMap<>(ReactionType.class))
                .put((ReactionType) row[1], (Long) row[2]));
        return result;
    }
}
