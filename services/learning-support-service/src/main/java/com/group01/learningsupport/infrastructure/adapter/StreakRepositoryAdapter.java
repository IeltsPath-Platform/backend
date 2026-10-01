package com.group01.learningsupport.infrastructure.adapter;

import com.group01.learningsupport.domain.aggregate.Streak;
import com.group01.learningsupport.domain.repository.StreakRepository;
import com.group01.learningsupport.infrastructure.persistence.JpaSupport;
import com.group01.learningsupport.infrastructure.persistence.entity.StreakJpaEntity;
import com.group01.learningsupport.infrastructure.persistence.mapper.StreakMapper;
import com.group01.learningsupport.infrastructure.persistence.repository.StreakJpaRepository;
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
