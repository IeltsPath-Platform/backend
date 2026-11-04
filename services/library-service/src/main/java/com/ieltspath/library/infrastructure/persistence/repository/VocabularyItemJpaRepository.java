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

    @EntityGraph(attributePaths = {"senses"})
    List<VocabularyItemJpaEntity> findAllByNormalizedLemmaIn(Collection<String> normalizedLemmas);

    @Override
    @EntityGraph(attributePaths = {"senses"})
    Optional<VocabularyItemJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"senses"})
    List<VocabularyItemJpaEntity> findAllByIdIn(List<UUID> ids);

    /**
     * Ids of the active words whose lemma contains the text: the exact word first, then words starting with it, then the
     * rest, at most {@code limit}. It reads {@code normalized_lemma} so the trigram and prefix indexes of V36 apply.
     * INACTIVE items stay out: the pronunciation rows of the dictation words ("the", "going"...) are such items.
     * {@code text} is lower-cased and trimmed; {@code prefix} and {@code contains} are LIKE patterns with it escaped.
     */
    @Query(nativeQuery = true, value = """
            SELECT i.id FROM vocabulary_items i
            WHERE i.status = 'ACTIVE' AND i.normalized_lemma LIKE :contains ESCAPE '\\'
            ORDER BY CASE WHEN i.normalized_lemma = :text THEN 0
                          WHEN i.normalized_lemma LIKE :prefix ESCAPE '\\' THEN 1 ELSE 2 END,
                     i.normalized_lemma, i.id
            LIMIT :limit
            """)
    List<UUID> searchIdsByLemma(@Param("text") String text, @Param("prefix") String prefix,
                                @Param("contains") String contains, @Param("limit") int limit);


    @Query("""
            SELECT sense.id AS senseId, item.id AS vocabularyItemId, item.lemma AS lemma,
                   item.ipa AS ipa, item.pronunciationAudioReference AS pronunciationAudioReference,
                   sense.partOfSpeech AS partOfSpeech, sense.englishDefinition AS englishDefinition,
                   sense.vietnameseMeaning AS vietnameseMeaning, sense.exampleSentence AS exampleSentence,
                   sense.imageUrl AS imageUrl
            FROM VocabularySenseJpaEntity sense, VocabularyItemJpaEntity item
            WHERE sense.vocabularyItemId = item.id
              AND sense.id IN :senseIds
              AND sense.status = com.ieltspath.library.domain.vo.ContentStatus.ACTIVE
              AND item.status = com.ieltspath.library.domain.vo.ContentStatus.ACTIVE
            """)
    List<VocabularySenseCardProjection> findActiveSenseCardsByIds(@Param("senseIds") Collection<UUID> senseIds);

    interface VocabularySenseCardProjection {
        UUID getSenseId();
        UUID getVocabularyItemId();
        String getLemma();
        String getIpa();
        String getPronunciationAudioReference();
        com.ieltspath.library.domain.vo.PartOfSpeech getPartOfSpeech();
        String getEnglishDefinition();
        String getVietnameseMeaning();
        String getExampleSentence();
        String getImageUrl();
    }

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
