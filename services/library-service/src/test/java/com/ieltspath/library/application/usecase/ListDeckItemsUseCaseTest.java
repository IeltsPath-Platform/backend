package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.port.FlashcardVocabularyMetadataReader;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.DeckItemResult;
import com.ieltspath.library.application.result.FlashcardVocabularyMetadata;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.aggregate.DeckItem;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.OwnedPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class ListDeckItemsUseCaseTest {
    private FlashcardDeckRepository decks;
    private FlashcardDeckItemRepository items;
    private FlashcardImageStorage storage;
    private FlashcardVocabularyMetadataReader metadataReader;
    private ListDeckItemsUseCase useCase;

    @BeforeEach
    void setUp() {
        decks = mock(FlashcardDeckRepository.class);
        items = mock(FlashcardDeckItemRepository.class);
        storage = mock(FlashcardImageStorage.class);
        metadataReader = mock(FlashcardVocabularyMetadataReader.class);
        useCase = new ListDeckItemsUseCase(decks, items, storage, metadataReader);
    }

    @Test
    void executeEnrichesDeckItemsWithIpaLeitnerBoxAndFallbackExampleSentence() {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID senseId1 = UUID.randomUUID();
        UUID senseId2 = UUID.randomUUID();

        FlashcardDeck deck = FlashcardDeck.create(userId, "IELTS Deck", "desc");
        when(decks.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(deck));

        DeckItem item1 = new DeckItem(
                deckId, UUID.randomUUID(), 1, Instant.now(), "front1", "back1",
                FlashcardSourceType.MANUAL, LibraryStatus.ACTIVE, "images/u1/card1.png",
                null, 3, senseId1
        );
        DeckItem item2 = new DeckItem(
                deckId, UUID.randomUUID(), 2, Instant.now(), "front2", "back2",
                FlashcardSourceType.MANUAL, LibraryStatus.ACTIVE, null,
                "Custom example sentence", 5, senseId2
        );
        DeckItem item3 = new DeckItem(
                deckId, UUID.randomUUID(), 3, Instant.now(), "front3", "back3",
                FlashcardSourceType.MANUAL, LibraryStatus.ACTIVE, null,
                null, 1, null
        );

        when(items.findActive(deckId, userId, 0, 10))
                .thenReturn(new OwnedPage<>(List.of(item1, item2, item3), 3));

        when(metadataReader.findBySenseIds(List.of(senseId1, senseId2))).thenReturn(Map.of(
                senseId1, new FlashcardVocabularyMetadata(senseId1, "/aɪ.piː.eɪ/", null, "Fallback sentence for 1"),
                senseId2, new FlashcardVocabularyMetadata(senseId2, "/tuː/", null, "Fallback sentence for 2")
        ));

        when(storage.publicUrl("images/u1/card1.png")).thenReturn("https://storage/card1.png");

        PageResult<DeckItemResult> result = useCase.execute(userId, deckId, new PageQuery(0, 10));

        assertEquals(3, result.items().size());
        assertEquals(3, result.total());

        // Item 1: null card example -> fallback to sense example, ipa present, leitnerBox 3
        DeckItemResult r1 = result.items().get(0);
        assertEquals("https://storage/card1.png", r1.imageUrl());
        assertEquals("Fallback sentence for 1", r1.exampleSentence());
        assertEquals("/aɪ.piː.eɪ/", r1.ipa());
        assertEquals(3, r1.leitnerBox());

        // Item 2: custom card example kept, ipa present, leitnerBox 5
        DeckItemResult r2 = result.items().get(1);
        assertNull(r2.imageUrl());
        assertEquals("Custom example sentence", r2.exampleSentence());
        assertEquals("/tuː/", r2.ipa());
        assertEquals(5, r2.leitnerBox());

        // Item 3: no vocabulary sense -> null ipa, null example, leitnerBox 1
        DeckItemResult r3 = result.items().get(2);
        assertNull(r3.imageUrl());
        assertNull(r3.exampleSentence());
        assertNull(r3.ipa());
        assertEquals(1, r3.leitnerBox());

        // Verified batch call to metadataReader
        verify(metadataReader, times(1)).findBySenseIds(anyList());
    }
}
