package com.group01.content.infrastructure.persistence.mapper;

import com.group01.content.domain.aggregate.VocabularyItem;
import com.group01.content.domain.entity.VocabularySense;
import com.group01.content.infrastructure.persistence.entity.VocabularyItemJpaEntity;
import com.group01.content.infrastructure.persistence.entity.VocabularySenseJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VocabularyPersistenceMapper {

    public VocabularyItem toDomain(VocabularyItemJpaEntity entity) {
        if (entity == null) return null;
        List<VocabularySense> senses = new ArrayList<>();
        if (entity.getSenses() != null) {
            for (VocabularySenseJpaEntity s : entity.getSenses()) {
                senses.add(toSenseDomain(s));
            }
        }
        return new VocabularyItem(
                entity.getId(),
                entity.getLemma(),
                entity.getNormalizedLemma(),
                entity.getIpa(),
                entity.getPronunciationAudioReference(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                senses
        );
    }

    public VocabularyItemJpaEntity toEntity(VocabularyItem domain) {
        if (domain == null) return null;
        List<VocabularySenseJpaEntity> senseEntities = new ArrayList<>();
        if (domain.getSenses() != null) {
            for (VocabularySense s : domain.getSenses()) {
                senseEntities.add(toSenseEntity(s));
            }
        }
        return VocabularyItemJpaEntity.builder()
                .id(domain.getId())
                .lemma(domain.getLemma())
                .normalizedLemma(domain.getNormalizedLemma())
                .ipa(domain.getIpa())
                .pronunciationAudioReference(domain.getPronunciationAudioReference())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .senses(senseEntities)
                .build();
    }

    public VocabularySense toSenseDomain(VocabularySenseJpaEntity entity) {
        if (entity == null) return null;
        return new VocabularySense(
                entity.getId(),
                entity.getVocabularyItemId(),
                entity.getPartOfSpeech(),
                entity.getEnglishDefinition(),
                entity.getVietnameseMeaning(),
                entity.getExampleSentence(),
                entity.getImageUrl(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public VocabularySenseJpaEntity toSenseEntity(VocabularySense domain) {
        if (domain == null) return null;
        return VocabularySenseJpaEntity.builder()
                .id(domain.getId())
                .vocabularyItemId(domain.getVocabularyItemId())
                .partOfSpeech(domain.getPartOfSpeech())
                .englishDefinition(domain.getEnglishDefinition())
                .vietnameseMeaning(domain.getVietnameseMeaning())
                .exampleSentence(domain.getExampleSentence())
                .imageUrl(domain.getImageUrl())
                .sortOrder(domain.getSortOrder())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

