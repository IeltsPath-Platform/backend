package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query("SELECT e FROM OutboxEventJpaEntity e WHERE e.publishedAt IS NULL ORDER BY e.createdAt ASC")
    List<OutboxEventJpaEntity> findPendingEvents(Pageable pageable);
}

