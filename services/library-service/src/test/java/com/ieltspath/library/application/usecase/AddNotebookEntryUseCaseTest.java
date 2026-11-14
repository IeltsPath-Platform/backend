package com.ieltspath.library.application.usecase;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import com.ieltspath.library.application.port.VocabularyNotebookReader;
import com.ieltspath.library.application.result.NotebookEntryResult;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.VocabularySenseCard;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddNotebookEntryUseCaseTest {
    @Test void savingAnAlreadySavedSenseReturnsTheExistingEntry() {
        FlashcardRepository cards = mock(FlashcardRepository.class);
        VocabularyRepository vocabulary = mock(VocabularyRepository.class);
        VocabularyNotebookReader notebookReader = mock(VocabularyNotebookReader.class);
        UUID user = UUID.randomUUID(), senseId = UUID.randomUUID(), itemId = UUID.randomUUID(), cardId = UUID.randomUUID();
        var sense = new VocabularySenseCard(senseId, itemId, "apple", "ipa", "audio", com.ieltspath.library.domain.vo.PartOfSpeech.NOUN,
                "a fruit", "quả táo", "An apple.", null);
        var card = Flashcard.builder().id(cardId).userId(user).sourceType(FlashcardSourceType.VOCABULARY_SENSE)
                .vocabularySenseId(senseId).sourceReferenceId(senseId).front("apple").back("quả táo")
                .status(LibraryStatus.ACTIVE).createdAt(Instant.parse("2026-10-26T00:00:00Z")).leitnerBox(2).build();
        when(vocabulary.findActiveSensesByIds(List.of(senseId))).thenReturn(List.of(sense));
        when(cards.findLiveVocabularySenseCard(user, senseId)).thenReturn(Optional.of(card));
        when(notebookReader.findEntry(user, cardId)).thenReturn(Optional.of(new NotebookEntryResult(cardId, senseId, itemId,
                "apple", "ipa", "audio", "NOUN", "quả táo", "a fruit", "An apple.", null,
                com.ieltspath.library.domain.vo.UnderstandingLevel.FAMILIAR, 2, 0, null, null,
                Instant.parse("2026-10-26T00:00:00Z"), "note")));

        var result = new AddNotebookEntryUseCase(cards, vocabulary, notebookReader, mock(TransactionTemplate.class)).execute(user, senseId);

        assertFalse(result.created());
        assertEquals(cardId, result.entry().flashcardId());
        assertEquals("apple", result.entry().lemma());
        assertEquals("quả táo", result.entry().vietnameseMeaning());
        assertEquals("note", result.entry().note());
        verify(cards, never()).save(any());
    }

    @Test void savingAnArchivedSenseRestoresItsExistingCard() {
        FlashcardRepository cards = mock(FlashcardRepository.class);
        VocabularyRepository vocabulary = mock(VocabularyRepository.class);
        VocabularyNotebookReader notebookReader = mock(VocabularyNotebookReader.class);
        UUID user = UUID.randomUUID(), senseId = UUID.randomUUID(), itemId = UUID.randomUUID(), cardId = UUID.randomUUID();
        var sense = new VocabularySenseCard(senseId, itemId, "apple", "ipa", "audio",
                com.ieltspath.library.domain.vo.PartOfSpeech.NOUN, "a fruit", "quả táo", "An apple.", null);
        var card = Flashcard.builder().id(cardId).userId(user).sourceType(FlashcardSourceType.VOCABULARY_SENSE)
                .vocabularySenseId(senseId).sourceReferenceId(senseId).front("apple").back("quả táo")
                .status(LibraryStatus.ARCHIVED).createdAt(Instant.parse("2026-10-26T00:00:00Z")).leitnerBox(2).build();
        when(vocabulary.findActiveSensesByIds(List.of(senseId))).thenReturn(List.of(sense));
        when(cards.findLiveVocabularySenseCard(user, senseId)).thenReturn(Optional.of(card));
        when(cards.save(card)).thenReturn(card);
        when(notebookReader.findEntry(user, cardId)).thenReturn(Optional.of(new NotebookEntryResult(cardId, senseId, itemId,
                "apple", "ipa", "audio", "NOUN", "quả táo", "a fruit", "An apple.", null,
                com.ieltspath.library.domain.vo.UnderstandingLevel.FAMILIAR, 2, 0, null, null,
                Instant.parse("2026-10-26T00:00:00Z"), null)));

        var result = new AddNotebookEntryUseCase(cards, vocabulary, notebookReader, mock(TransactionTemplate.class)).execute(user, senseId);

        assertFalse(result.created());
        assertEquals(LibraryStatus.ACTIVE, card.getStatus());
        verify(cards).save(card);
    }

    private static VocabularySenseCard apple(UUID senseId, UUID itemId) {
        return new VocabularySenseCard(senseId, itemId, "apple", "ipa", "audio", com.ieltspath.library.domain.vo.PartOfSpeech.NOUN,
                "a fruit", "quả táo", "An apple.", null);
    }

    private static NotebookEntryResult entry(UUID cardId, UUID senseId, UUID itemId) {
        return new NotebookEntryResult(cardId, senseId, itemId, "apple", "ipa", "audio", "NOUN", "quả táo", "a fruit",
                "An apple.", null, com.ieltspath.library.domain.vo.UnderstandingLevel.UNKNOWN, 1, 0, null, null,
                Instant.parse("2026-10-26T00:00:00Z"), null);
    }

    @SuppressWarnings("unchecked")
    private static TransactionTemplate runningTransactions() {
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(call ->
                ((org.springframework.transaction.support.TransactionCallback<Object>) call.getArgument(0)).doInTransaction(null));
        return tx;
    }

    @Test void theFirstSaveCreatesAVocabularySenseCard() {
        FlashcardRepository cards = mock(FlashcardRepository.class);
        VocabularyRepository vocabulary = mock(VocabularyRepository.class);
        VocabularyNotebookReader notebookReader = mock(VocabularyNotebookReader.class);
        UUID user = UUID.randomUUID(), senseId = UUID.randomUUID(), itemId = UUID.randomUUID();
        when(vocabulary.findActiveSensesByIds(List.of(senseId))).thenReturn(List.of(apple(senseId, itemId)));
        when(cards.findLiveVocabularySenseCard(user, senseId)).thenReturn(Optional.empty());
        when(cards.save(any())).thenAnswer(call -> call.getArgument(0));
        when(notebookReader.findEntry(eq(user), any())).thenAnswer(call -> Optional.of(entry(call.getArgument(1), senseId, itemId)));

        var result = new AddNotebookEntryUseCase(cards, vocabulary, notebookReader, runningTransactions()).execute(user, senseId);

        assertTrue(result.created());
        var saved = org.mockito.ArgumentCaptor.forClass(Flashcard.class);
        verify(cards).save(saved.capture());
        assertEquals(FlashcardSourceType.VOCABULARY_SENSE, saved.getValue().getSourceType());
        assertEquals(senseId, saved.getValue().getVocabularySenseId());
        assertEquals("apple", saved.getValue().getFront());
        assertEquals("quả táo", saved.getValue().getBack());
    }

    @Test void anUnknownOrInactiveSenseIsNotFound() {
        FlashcardRepository cards = mock(FlashcardRepository.class);
        VocabularyRepository vocabulary = mock(VocabularyRepository.class);
        UUID senseId = UUID.randomUUID();
        when(vocabulary.findActiveSensesByIds(List.of(senseId))).thenReturn(List.of());

        assertThrows(com.ieltspath.library.domain.exception.ResourceNotFoundException.class, () -> new AddNotebookEntryUseCase(
                cards, vocabulary, mock(VocabularyNotebookReader.class), runningTransactions()).execute(UUID.randomUUID(), senseId));
        verify(cards, never()).save(any());
    }

    @Test void aConcurrentSaveThatWinsIsReturned() {
        FlashcardRepository cards = mock(FlashcardRepository.class);
        VocabularyRepository vocabulary = mock(VocabularyRepository.class);
        VocabularyNotebookReader notebookReader = mock(VocabularyNotebookReader.class);
        UUID user = UUID.randomUUID(), senseId = UUID.randomUUID(), itemId = UUID.randomUUID(), cardId = UUID.randomUUID();
        var winner = Flashcard.builder().id(cardId).userId(user).sourceType(FlashcardSourceType.VOCABULARY_SENSE)
                .vocabularySenseId(senseId).sourceReferenceId(senseId).front("apple").back("quả táo")
                .status(LibraryStatus.ACTIVE).createdAt(Instant.parse("2026-10-26T00:00:00Z")).leitnerBox(1).build();
        when(vocabulary.findActiveSensesByIds(List.of(senseId))).thenReturn(List.of(apple(senseId, itemId)));
        when(cards.findLiveVocabularySenseCard(user, senseId)).thenReturn(Optional.empty(), Optional.of(winner));
        when(cards.save(any())).thenThrow(new com.ieltspath.library.domain.exception.ConflictException());
        when(notebookReader.findEntry(user, cardId)).thenReturn(Optional.of(entry(cardId, senseId, itemId)));

        var result = new AddNotebookEntryUseCase(cards, vocabulary, notebookReader, runningTransactions()).execute(user, senseId);

        assertFalse(result.created());
        assertEquals(cardId, result.entry().flashcardId());
    }
}
