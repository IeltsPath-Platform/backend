package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.TopicJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicJpaRepository extends JpaRepository<TopicJpaEntity, UUID> {
    Optional<TopicJpaEntity> findByCode(String code);
    List<TopicJpaEntity> findByParentTopicIdIsNullOrderBySortOrderAsc();
    List<TopicJpaEntity> findByParentTopicIdOrderBySortOrderAsc(UUID parentTopicId);
    boolean existsByCode(String code);
}

