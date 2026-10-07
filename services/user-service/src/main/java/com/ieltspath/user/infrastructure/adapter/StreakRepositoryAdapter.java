package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.Streak;
import com.ieltspath.user.domain.repository.StreakRepository;
import com.ieltspath.user.infrastructure.persistence.JpaSupport;
import com.ieltspath.user.infrastructure.persistence.entity.StreakJpaEntity;
import com.ieltspath.user.infrastructure.persistence.mapper.StreakMapper;
import com.ieltspath.user.infrastructure.persistence.repository.StreakJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StreakRepositoryAdapter implements StreakRepository {
    private final StreakJpaRepository repository;
    private final StreakMapper mapper;

    @Override
    public Optional<Streak> findByUserId(UUID userId) {
        return repository.findById(userId).map(mapper::toDomain);
    }

    @Override
    public Streak save(Streak streak) {
        StreakJpaEntity entity = repository.findById(streak.getUserId()).orElseGet(StreakJpaEntity::new);
        mapper.copy(streak, entity);
        return mapper.toDomain(JpaSupport.flush(repository, entity));
    }
}
