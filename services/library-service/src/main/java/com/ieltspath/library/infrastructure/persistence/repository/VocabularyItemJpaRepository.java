package com.ieltspath.library.infrastructure.persistence.repository;

import com.ieltspath.library.infrastructure.persistence.entity.VocabularyItemJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
    // INACTIVE items stay out of the search: the pronunciation rows of the dictation words ("the", "going"...) are such items.
    @Query("SELECT v FROM VocabularyItemJpaEntity v WHERE v.status = com.ieltspath.library.domain.vo.ContentStatus.ACTIVE "
            + "AND LOWER(v.lemma) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY v.lemma ASC")
    List<VocabularyItemJpaEntity> searchByLemma(@Param("query") String query);

    boolean existsByNormalizedLemma(String normalizedLemma);

    /** Pronunciations by word, for the dictation hints: any status, only items that have an IPA. */
    @Query("SELECT v.normalizedLemma AS word, v.ipa AS ipa FROM VocabularyItemJpaEntity v "
            + "WHERE v.normalizedLemma IN :words AND v.ipa IS NOT NULL")
    List<WordIpa> findIpaByNormalizedLemmaIn(@Param("words") Collection<String> words);

    interface WordIpa {
        String getWord();

        String getIpa();
    }
}
