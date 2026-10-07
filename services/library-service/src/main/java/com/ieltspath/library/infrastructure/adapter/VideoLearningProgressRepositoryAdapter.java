package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.domain.aggregate.VideoLearningProgress;
import com.ieltspath.library.domain.repository.VideoLearningProgressRepository;
import com.ieltspath.library.domain.vo.OwnedPage;
import com.ieltspath.library.infrastructure.persistence.JpaSupport;
import com.ieltspath.library.infrastructure.persistence.entity.VideoLearningProgressJpaEntity;
import com.ieltspath.library.infrastructure.persistence.mapper.VideoLearningProgressMapper;
import com.ieltspath.library.infrastructure.persistence.repository.VideoLearningProgressJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VideoLearningProgressRepositoryAdapter implements VideoLearningProgressRepository {
    private final VideoLearningProgressJpaRepository repository;
    private final VideoLearningProgressMapper mapper;

    @Override
    public Optional<VideoLearningProgress> findByUserIdAndVideoId(UUID userId, UUID videoId) {
        return repository.findByUserIdAndVideoId(userId, videoId).map(mapper::toDomain);
    }

    @Override
    public Optional<VideoLearningProgress> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public OwnedPage<VideoLearningProgress> findByUserId(UUID userId, int page, int size) {
        return JpaSupport.page(
                repository.findByUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))),
                mapper::toDomain
        );
    }

    @Override
    public VideoLearningProgress save(VideoLearningProgress progress) {
        VideoLearningProgressJpaEntity entity = repository.findById(progress.getId())
                .orElseGet(VideoLearningProgressJpaEntity::new);
        mapper.copy(progress, entity);
        return mapper.toDomain(JpaSupport.flush(repository, entity));
    }

    @Override
    public void delete(VideoLearningProgress progress) {
        repository.deleteById(progress.getId());
    }
}
