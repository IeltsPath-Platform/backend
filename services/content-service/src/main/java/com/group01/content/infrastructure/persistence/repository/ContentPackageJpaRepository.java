package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.infrastructure.persistence.entity.ContentPackageJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentPackageJpaRepository extends JpaRepository<ContentPackageJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"versions"})
    Optional<ContentPackageJpaEntity> findByCode(String code);

    @Override
    @EntityGraph(attributePaths = {"versions"})
    Optional<ContentPackageJpaEntity> findById(UUID id);

    @Query("SELECT p FROM ContentPackageJpaEntity p WHERE (:accessLevel IS NULL OR p.accessLevel = :accessLevel) AND (:status IS NULL OR p.status = :status) ORDER BY p.createdAt DESC")
    List<ContentPackageJpaEntity> findAllFiltered(@Param("accessLevel") AccessLevel accessLevel,
                                                  @Param("status") PublicationStatus status);

    boolean existsByCode(String code);
}

