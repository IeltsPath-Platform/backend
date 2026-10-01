package com.group01.content.application.usecase;

import com.group01.content.application.result.VocabularyItemResult;
import com.group01.content.application.result.VocabularySenseResult;
import com.group01.content.domain.aggregate.VocabularyItem;
import com.group01.content.domain.exception.VocabularyNotFoundException;
import com.group01.content.domain.repository.VocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetVocabularyDetailUseCase {

    private final VocabularyRepository vocabularyRepository;

    public GetVocabularyDetailUseCase(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    public VocabularyItemResult execute(UUID id) {
        VocabularyItem item = vocabularyRepository.findById(id)
                .orElseThrow(() -> new VocabularyNotFoundException(id));

        return new VocabularyItemResult(
                item.getId(),
                item.getLemma(),
                item.getNormalizedLemma(),
                item.getIpa(),
                item.getPronunciationAudioReference(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getSenses().stream()
                        .map(s -> new VocabularySenseResult(
                                s.getId(),
                                s.getVocabularyItemId(),
                                s.getPartOfSpeech(),
                                s.getEnglishDefinition(),
                                s.getVietnameseMeaning(),
                                s.getExampleSentence(),
                                s.getImageUrl(),
                                s.getSortOrder(),
                                s.getStatus(),
                                s.getCreatedAt(),
                                s.getUpdatedAt()
                        ))
                        .toList()
        );
    }
}

