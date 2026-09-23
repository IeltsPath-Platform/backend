package com.group01.access.infrastructure.persistence.repository;

import com.group01.access.infrastructure.persistence.entity.SubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

    @Query("SELECT s FROM SubscriptionJpaEntity s WHERE s.userId = :userId AND s.status = 'ACTIVE' AND (s.endsAt IS NULL OR s.endsAt > :now) ORDER BY s.endsAt DESC LIMIT 1")
    Optional<SubscriptionJpaEntity> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    List<SubscriptionJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
