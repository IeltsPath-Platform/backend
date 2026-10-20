package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.domain.aggregate.VideoDictationAttempt;
import com.ieltspath.library.domain.repository.VideoDictationAttemptRepository;
import com.ieltspath.library.infrastructure.persistence.JpaSupport;
import com.ieltspath.library.infrastructure.persistence.entity.VideoDictationAttemptJpaEntity;
import com.ieltspath.library.infrastructure.persistence.mapper.VideoDictationAttemptMapper;
import com.ieltspath.library.infrastructure.persistence.repository.VideoDictationAttemptJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VideoDictationAttemptRepositoryAdapter implements VideoDictationAttemptRepository {
    private final VideoDictationAttemptJpaRepository repository;
    private final VideoDictationAttemptMapper mapper;

    @Override
    public VideoDictationAttempt save(VideoDictationAttempt attempt) {
        VideoDictationAttemptJpaEntity entity = repository.findById(attempt.getId())
                .orElseGet(VideoDictationAttemptJpaEntity::new);
        mapper.copy(attempt, entity);
        return mapper.toDomain(JpaSupport.flush(repository, entity));
    }

    @Override
    public int maxAttemptNo(UUID userId, UUID segmentId) {
        return repository.maxAttemptNo(userId, segmentId);
    }

    @Override
    public List<VideoDictationAttempt> findByUserIdAndVideoId(UUID userId, UUID videoId) {
        return repository.findByUserIdAndVideoIdOrderByCreatedAtAscAttemptNoAsc(userId, videoId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Map<UUID, Integer> countPassedSegmentsByVideo(UUID userId, Collection<UUID> videoIds) {
        Map<UUID, Integer> passed = new HashMap<>();
        if (!videoIds.isEmpty()) {
            repository.countPassedByVideo(userId, videoIds)
                    .forEach(count -> passed.put(count.getVideoId(), (int) count.getPassed()));
        }
        return passed;
    }

    @Override
    public Map<UUID, Instant> lastAttemptAtByVideo(UUID userId, Collection<UUID> videoIds) {
        Map<UUID, Instant> lastAt = new HashMap<>();
        if (!videoIds.isEmpty()) {
            repository.lastAttemptAtByVideo(userId, videoIds)
                    .forEach(last -> lastAt.put(last.getVideoId(), last.getLastAt()));
        }
        return lastAt;
    }
}
