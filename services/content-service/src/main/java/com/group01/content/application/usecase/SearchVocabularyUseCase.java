package com.group01.content.application.usecase;

import com.group01.content.application.result.VocabularyItemResult;
import com.group01.content.application.result.VocabularySenseResult;
import com.group01.content.domain.aggregate.VocabularyItem;
import com.group01.content.domain.repository.VocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchVocabularyUseCase {

    private final VocabularyRepository vocabularyRepository;

    public SearchVocabularyUseCase(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    public List<VocabularyItemResult> execute(String query) {
        List<VocabularyItem> items = vocabularyRepository.searchByLemma(query != null ? query.trim() : "");
        return items.stream()
                .map(item -> new VocabularyItemResult(
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
                ))
                .toList();
    }
}

