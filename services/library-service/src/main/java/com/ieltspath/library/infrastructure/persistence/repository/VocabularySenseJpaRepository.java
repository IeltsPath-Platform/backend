package com.ieltspath.library.infrastructure.persistence.repository;

import com.ieltspath.library.infrastructure.persistence.entity.VocabularySenseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VocabularySenseJpaRepository extends JpaRepository<VocabularySenseJpaEntity, UUID> {
    @Query("""
            select sense.id as senseId, item.ipa as ipa,
                   item.pronunciationAudioReference as pronunciationAudioReference,
                   sense.exampleSentence as exampleSentence
            from VocabularySenseJpaEntity sense, VocabularyItemJpaEntity item
            where sense.vocabularyItemId = item.id and sense.id in :senseIds
            """)
    List<VocabularyFlashcardMetadataProjection> findFlashcardMetadataBySenseIds(
            @Param("senseIds") Collection<UUID> senseIds);
}
