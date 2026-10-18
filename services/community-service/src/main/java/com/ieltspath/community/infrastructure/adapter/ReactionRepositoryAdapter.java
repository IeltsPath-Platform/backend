package com.ieltspath.community.infrastructure.adapter;

import com.ieltspath.community.domain.repository.ReactionRepository;
import com.ieltspath.community.domain.vo.ReactionType;
import com.ieltspath.community.infrastructure.persistence.entity.PostReactionId;
import com.ieltspath.community.infrastructure.persistence.repository.PostReactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class ReactionRepositoryAdapter implements ReactionRepository {
    private final PostReactionJpaRepository repository;

    public boolean add(UUID postId, UUID userId, ReactionType type) {
        return repository.insertOrReplace(postId, userId, type.name()) > 0;
    }

    public boolean remove(UUID postId, UUID userId, ReactionType type) {
        return repository.remove(postId, userId, type) > 0;
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
