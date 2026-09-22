package com.group01.content.infrastructure.persistence.adapter;

import com.group01.content.domain.aggregate.LearningVideo;
import com.group01.content.domain.repository.LearningVideoRepository;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.infrastructure.persistence.mapper.LearningVideoPersistenceMapper;
import com.group01.content.infrastructure.persistence.repository.LearningVideoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LearningVideoRepositoryAdapter implements LearningVideoRepository {

    private final LearningVideoJpaRepository learningVideoJpaRepository;
    private final LearningVideoPersistenceMapper mapper;

    public LearningVideoRepositoryAdapter(LearningVideoJpaRepository learningVideoJpaRepository,
                                          LearningVideoPersistenceMapper mapper) {
        this.learningVideoJpaRepository = learningVideoJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public LearningVideo save(LearningVideo video) {
        var entity = mapper.toEntity(video);
        var saved = learningVideoJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<LearningVideo> findById(UUID id) {
        return learningVideoJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LearningVideo> findByYoutubeVideoId(String youtubeVideoId) {
        return learningVideoJpaRepository.findByYoutubeVideoId(youtubeVideoId).map(mapper::toDomain);
    }

    @Override
    public List<LearningVideo> findAll(AccessLevel accessLevel, PublicationStatus status) {
        return learningVideoJpaRepository.findAllFiltered(accessLevel, status).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByYoutubeVideoId(String youtubeVideoId) {
        return learningVideoJpaRepository.existsByYoutubeVideoId(youtubeVideoId);
    }
}

