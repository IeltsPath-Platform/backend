package com.ieltspath.community.infrastructure.adapter;

import com.ieltspath.community.domain.repository.CommentReactionRepository;
import com.ieltspath.community.domain.vo.CommentInteractions;
import com.ieltspath.community.domain.vo.ReactionType;
import com.ieltspath.community.infrastructure.persistence.repository.CommentReactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class CommentReactionRepositoryAdapter implements CommentReactionRepository {
    private final CommentReactionJpaRepository repository;

    public boolean add(UUID commentId, UUID userId, ReactionType type) {
        return repository.insertOrReplace(commentId, userId, type.name()) > 0;
    }

    public boolean remove(UUID commentId, UUID userId, ReactionType type) {
        return repository.remove(commentId, userId, type) > 0;
    }

    public Map<UUID, Map<ReactionType, Long>> counts(Set<UUID> commentIds) {
        Map<UUID, Map<ReactionType, Long>> result = new HashMap<>();
        if (!commentIds.isEmpty()) {
            repository.countByComments(commentIds).forEach(row -> result
                    .computeIfAbsent((UUID) row[0], ignored -> new EnumMap<>(ReactionType.class))
                    .put((ReactionType) row[1], ((Number) row[2]).longValue()));
        }
        return result;
    }

    public Map<UUID, ReactionType> mine(Set<UUID> commentIds, UUID viewer) {
        Map<UUID, ReactionType> result = new HashMap<>();
        if (!commentIds.isEmpty()) {
            repository.findMine(commentIds, viewer).forEach(row -> result.put((UUID) row[0], (ReactionType) row[1]));
        }
        return result;
    }

    public Map<UUID, CommentInteractions> interactions(Set<UUID> commentIds, UUID viewer) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Map<ReactionType, Long>> counts = new HashMap<>();
        Map<UUID, ReactionType> mine = new HashMap<>();
        for (var row : repository.interactions(commentIds, viewer)) {
            UUID comment = (UUID) row[0];
            ReactionType type = ReactionType.valueOf((String) row[1]);
            counts.computeIfAbsent(comment, ignored -> new EnumMap<>(ReactionType.class))
                    .put(type, ((Number) row[2]).longValue());
            if (Boolean.TRUE.equals(row[3])) {
                mine.put(comment, type);
            }
        }
        Map<UUID, CommentInteractions> result = new HashMap<>();
        counts.forEach((comment, values) -> result.put(comment, new CommentInteractions(values, mine.get(comment))));
        return result;
    }
}
