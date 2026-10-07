package com.ieltspath.content.infrastructure.persistence.repository;

import com.ieltspath.content.infrastructure.persistence.entity.ContentAssetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContentAssetJpaRepository extends JpaRepository<ContentAssetJpaEntity, UUID> {
}

