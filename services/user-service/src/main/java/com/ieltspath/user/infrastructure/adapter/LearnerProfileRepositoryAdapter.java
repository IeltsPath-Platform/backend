package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.infrastructure.persistence.entity.LearnerProfileJpaEntity;
import com.ieltspath.user.infrastructure.persistence.mapper.LearnerProfileMapper;
import com.ieltspath.user.infrastructure.persistence.repository.LearnerProfileJpaRepository;
import com.ieltspath.user.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public LearnerProfile save(LearnerProfile profile) {
        LearnerProfileJpaEntity entity = learnerProfileMapper.toEntity(profile);
        if (entity.getUser() == null && profile.getUserId() != null) {
            entity.setUser(userJpaRepository.getReferenceById(profile.getUserId()));
        }
        // The id is assigned (@MapsId), so save() would merge a row that does not exist yet and Hibernate
        // rejects that as a stale update. Insert explicitly the first time.
        if (!learnerProfileJpaRepository.existsByUserId(entity.getUserId())) {
            entityManager.persist(entity);
            return learnerProfileMapper.toDomain(entity);
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

