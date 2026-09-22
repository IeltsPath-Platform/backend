package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.ContentAssetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContentAssetJpaRepository extends JpaRepository<ContentAssetJpaEntity, UUID> {
}

