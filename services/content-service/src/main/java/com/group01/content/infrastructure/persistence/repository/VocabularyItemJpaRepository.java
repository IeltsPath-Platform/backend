package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.VocabularyItemJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VocabularyItemJpaRepository extends JpaRepository<VocabularyItemJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"senses"})
    Optional<VocabularyItemJpaEntity> findByNormalizedLemma(String normalizedLemma);

    @Override
    @EntityGraph(attributePaths = {"senses"})
    Optional<VocabularyItemJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"senses"})
    List<VocabularyItemJpaEntity> findAllByIdIn(List<UUID> ids);

    @EntityGraph(attributePaths = {"senses"})
    @Query("SELECT v FROM VocabularyItemJpaEntity v WHERE LOWER(v.lemma) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY v.lemma ASC")
    List<VocabularyItemJpaEntity> searchByLemma(@Param("query") String query);

    boolean existsByNormalizedLemma(String normalizedLemma);
}

