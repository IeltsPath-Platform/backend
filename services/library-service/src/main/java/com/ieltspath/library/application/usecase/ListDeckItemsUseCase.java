package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.port.FlashcardVocabularyMetadataReader;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.DeckItemResult;
import com.ieltspath.library.application.result.FlashcardVocabularyMetadata;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.aggregate.DeckItem;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.OwnedPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListDeckItemsUseCase {
    private final FlashcardDeckRepository decks;
    private final FlashcardDeckItemRepository items;
    private final FlashcardImageStorage storage;
    private final FlashcardVocabularyMetadataReader vocabularyMetadataReader;

    @Transactional(readOnly = true)
    public PageResult<DeckItemResult> execute(UUID userId, UUID deckId, PageQuery query) {
        ApplicationSupport.required(decks.findByIdAndUserId(deckId, userId));
        OwnedPage<DeckItem> ownedPage = items.findActive(deckId, userId, query.page(), query.size());
        List<UUID> senseIds = ownedPage.items().stream()
                .map(DeckItem::vocabularySenseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, FlashcardVocabularyMetadata> metadataBySense = vocabularyMetadataReader.findBySenseIds(senseIds);

        return ApplicationSupport.page(ownedPage, query)
                .map(item -> {
                    FlashcardVocabularyMetadata meta = item.vocabularySenseId() != null
                            ? metadataBySense.get(item.vocabularySenseId())
                            : null;
                    String ipa = meta != null ? meta.ipa() : null;
                    String fallbackSentence = meta != null ? meta.exampleSentence() : null;
                    DeckItem enriched = item.withMetadata(ipa, fallbackSentence);
                    return DeckItemResult.from(enriched, storage.publicUrl(item.imageKey()));
                });
    }
}
