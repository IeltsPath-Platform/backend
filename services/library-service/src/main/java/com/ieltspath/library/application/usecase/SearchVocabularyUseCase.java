package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.result.VocabularySenseResult;
import com.ieltspath.library.application.port.PronunciationAudioUrls;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchVocabularyUseCase {

    private final VocabularyRepository vocabularyRepository;
    private final PronunciationAudioUrls audioUrls;

    public SearchVocabularyUseCase(VocabularyRepository vocabularyRepository, PronunciationAudioUrls audioUrls) {
        this.vocabularyRepository = vocabularyRepository;
        this.audioUrls = audioUrls;
    }

    public List<VocabularyItemResult> execute(String query) {
        List<VocabularyItem> items = vocabularyRepository.searchByLemma(query != null ? query.trim() : "");
        return items.stream()
                .map(item -> new VocabularyItemResult(
                        item.getId(),
                        item.getLemma(),
                        item.getNormalizedLemma(),
                        item.getIpa(),
                        audioUrls.resolve(item.preferredAudioReference()),
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
