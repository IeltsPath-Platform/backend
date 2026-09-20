package com.group01.user.infrastructure.adapter;

import com.group01.user.domain.aggregate.LearnerProfile;
import com.group01.user.domain.repository.LearnerProfileRepository;
import com.group01.user.infrastructure.persistence.entity.LearnerProfileJpaEntity;
import com.group01.user.infrastructure.persistence.mapper.LearnerProfileMapper;
import com.group01.user.infrastructure.persistence.repository.LearnerProfileJpaRepository;
import com.group01.user.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LearnerProfileRepositoryAdapter implements LearnerProfileRepository {
    private final LearnerProfileJpaRepository learnerProfileJpaRepository;
    private final LearnerProfileMapper learnerProfileMapper;
    private final UserJpaRepository userJpaRepository;

    @Override
    public LearnerProfile save(LearnerProfile profile) {
        LearnerProfileJpaEntity entity = learnerProfileMapper.toEntity(profile);
        if (entity.getUser() == null && profile.getUserId() != null) {
            entity.setUser(userJpaRepository.getReferenceById(profile.getUserId()));
        }
        return learnerProfileMapper.toDomain(learnerProfileJpaRepository.save(entity));
    }

    @Override
    public Optional<LearnerProfile> findByUserId(UUID userId) {
        return learnerProfileJpaRepository.findByUserId(userId).map(learnerProfileMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return learnerProfileJpaRepository.existsByUserId(userId);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        learnerProfileJpaRepository.deleteByUserId(userId);
    }
}

