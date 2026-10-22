package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.port.FlashcardVocabularyMetadataReader;
import com.ieltspath.library.application.query.DueFlashcardsQuery;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetDueFlashcardsUseCase {
    private final FlashcardRepository repository;
    private final FlashcardVocabularyMetadataReader vocabularyMetadataReader;
    private final FlashcardImageStorage storage;

    @Transactional(readOnly = true)
    public PageResult<FlashcardResult> execute(UUID userId, DueFlashcardsQuery query) {
        var duePage = repository.findDueCards(userId, query.deckId(), Instant.now(), query.page(), query.limit());
        List<UUID> senseIds = duePage.items().stream().map(Flashcard::getVocabularySenseId)
                .filter(id -> id != null).distinct().toList();
        var metadataBySense = vocabularyMetadataReader.findBySenseIds(senseIds);
        List<FlashcardResult> cards = duePage.items().stream()
                .map(card -> FlashcardResult.from(card, storage.publicUrl(card.getImageKey())))
                .map(result -> result.withVocabularyMetadata(result.vocabularySenseId() == null ? null
                        : metadataBySense.get(result.vocabularySenseId())))
                .toList();
        return new PageResult<>(cards, query.page(), query.limit(), duePage.total());
    }
}
