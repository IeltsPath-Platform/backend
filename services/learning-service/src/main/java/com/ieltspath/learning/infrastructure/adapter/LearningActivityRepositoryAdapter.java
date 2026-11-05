package com.ieltspath.learning.infrastructure.adapter;

import com.ieltspath.learning.domain.aggregate.LearningActivity;
import com.ieltspath.learning.domain.repository.LearningActivityRepository;
import com.ieltspath.learning.domain.vo.OwnedPage;
import com.ieltspath.learning.infrastructure.persistence.JpaSupport;
import com.ieltspath.learning.infrastructure.persistence.entity.LearningActivityJpaEntity;
import com.ieltspath.learning.infrastructure.persistence.mapper.LearningActivityMapper;
import com.ieltspath.learning.infrastructure.persistence.repository.LearningActivityJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LearningActivityRepositoryAdapter implements LearningActivityRepository {
    private final LearningActivityJpaRepository repository;
    private final LearningActivityMapper mapper;

    @Override
    public LearningActivity save(LearningActivity activity) {
        LearningActivityJpaEntity entity = repository.findById(activity.getId()).orElseGet(LearningActivityJpaEntity::new);
        mapper.copy(activity, entity);
        return mapper.toDomain(JpaSupport.flush(repository, entity));
    }

    @Override
    public OwnedPage<LearningActivity> findByUserId(UUID userId, int page, int size) {
        return JpaSupport.page(
                repository.findByUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"))),
                mapper::toDomain
        );
    }

    @Override
    public Optional<LearningActivity> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public void delete(LearningActivity activity) {
        repository.deleteById(activity.getId());
    }
}
