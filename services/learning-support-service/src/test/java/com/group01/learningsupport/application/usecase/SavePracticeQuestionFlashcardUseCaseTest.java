package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.result.SavedFlashcard;
import com.group01.learningsupport.domain.aggregate.Flashcard;
import com.group01.learningsupport.domain.exception.ConflictException;
import com.group01.learningsupport.domain.exception.InvalidDataException;
import com.group01.learningsupport.domain.repository.FlashcardRepository;
import com.group01.learningsupport.domain.vo.FlashcardSourceType;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SavePracticeQuestionFlashcardUseCaseTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();

    private Flashcard card() {
        return Flashcard.create(userId, FlashcardSourceType.PRACTICE_QUESTION, null, questionId, null, "Front", "Back");
    }

    @Test
    void practiceQuestionCardsNeedOnlyTheQuestionReference() {
        Flashcard card = card();
        assertEquals(questionId, card.getSourceReferenceId());
        assertNull(card.getVocabularySenseId());
        assertNull(card.getHighlightedText());

        assertThrows(InvalidDataException.class, () -> Flashcard.create(
                userId, FlashcardSourceType.PRACTICE_QUESTION, null, null, null, "Front", "Back"));
        assertThrows(InvalidDataException.class, () -> Flashcard.create(
                userId, FlashcardSourceType.PRACTICE_QUESTION, UUID.randomUUID(), questionId, null, "Front", "Back"));
        assertThrows(InvalidDataException.class, () -> Flashcard.create(
                userId, FlashcardSourceType.PRACTICE_QUESTION, null, questionId, "text", "Front", "Back"));
    }

    @Test
    void updatingKeepsThePracticeQuestionRules() {
        Flashcard card = card();
        card.update(FlashcardSourceType.PRACTICE_QUESTION, null, questionId, null, "New front", "Back",
                LibraryStatus.ARCHIVED);
        assertEquals("New front", card.getFront());
        assertThrows(InvalidDataException.class, () -> card.update(
                FlashcardSourceType.PRACTICE_QUESTION, null, null, null, "Front", "Back", LibraryStatus.ACTIVE));
    }

    @Test
    void theFirstSaveCreatesTheCard() {
        FlashcardRepository repository = mock(FlashcardRepository.class);
        when(repository.findLivePracticeQuestionCard(userId, questionId)).thenReturn(Optional.empty());
        when(repository.save(any(Flashcard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SavedFlashcard saved = new SavePracticeQuestionFlashcardUseCase(repository)
                .execute(userId, questionId, "Front", "Back");

        assertTrue(saved.created());
        assertEquals(FlashcardSourceType.PRACTICE_QUESTION, saved.flashcard().sourceType());
        assertEquals(questionId, saved.flashcard().sourceReferenceId());
    }

    @Test
    void savingAgainReturnsTheExistingCardWithoutSaving() {
        FlashcardRepository repository = mock(FlashcardRepository.class);
        Flashcard existing = card();
        when(repository.findLivePracticeQuestionCard(userId, questionId)).thenReturn(Optional.of(existing));

        SavedFlashcard saved = new SavePracticeQuestionFlashcardUseCase(repository)
                .execute(userId, questionId, "Other front", "Other back");

        assertFalse(saved.created());
        assertEquals(existing.getId(), saved.flashcard().id());
        verify(repository, never()).save(any());
    }

    @Test
    void savingAnArchivedCardAgainRestoresItToActive() {
        FlashcardRepository repository = mock(FlashcardRepository.class);
        Flashcard archived = card();
        archived.update(FlashcardSourceType.PRACTICE_QUESTION, null, questionId, null, "Front", "Back",
                LibraryStatus.ARCHIVED);
        when(repository.findLivePracticeQuestionCard(userId, questionId)).thenReturn(Optional.of(archived));
        when(repository.save(any(Flashcard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SavedFlashcard saved = new SavePracticeQuestionFlashcardUseCase(repository)
                .execute(userId, questionId, "Other front", "Other back");

        assertFalse(saved.created());
        assertEquals(archived.getId(), saved.flashcard().id());
        assertEquals(LibraryStatus.ACTIVE, saved.flashcard().status());
        // The first save's content is kept; only the status changes.
        assertEquals("Front", saved.flashcard().front());
        verify(repository).save(archived);
    }

    @Test
    void aConcurrentSaveThatWinsIsReadBack() {
        FlashcardRepository repository = mock(FlashcardRepository.class);
        Flashcard winner = card();
        when(repository.findLivePracticeQuestionCard(userId, questionId))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(repository.save(any(Flashcard.class))).thenThrow(new ConflictException());

        SavedFlashcard saved = new SavePracticeQuestionFlashcardUseCase(repository)
                .execute(userId, questionId, "Front", "Back");

        assertFalse(saved.created());
        assertEquals(winner.getId(), saved.flashcard().id());
    }

    @Test
    void aConflictWithNoCardToReadBackIsRethrown() {
        FlashcardRepository repository = mock(FlashcardRepository.class);
        ConflictException conflict = new ConflictException();
        when(repository.findLivePracticeQuestionCard(userId, questionId)).thenReturn(Optional.empty());
        when(repository.save(any(Flashcard.class))).thenThrow(conflict);

        ConflictException thrown = assertThrows(ConflictException.class, () ->
                new SavePracticeQuestionFlashcardUseCase(repository).execute(userId, questionId, "Front", "Back"));
        assertSame(conflict, thrown);
    }
}
