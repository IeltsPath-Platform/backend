package com.group01.library.application.usecase;

import com.group01.library.application.result.FlashcardResult;
import com.group01.library.application.result.SavedFlashcard;
import com.group01.library.domain.aggregate.Flashcard;
import com.group01.library.domain.exception.ConflictException;
import com.group01.library.domain.repository.FlashcardRepository;
import com.group01.library.domain.vo.FlashcardSourceType;
import com.group01.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Saves a tutor practice question as a flashcard at most once per learner.
 *
 * <p>Saving a question that already has an archived card brings that card back to ACTIVE instead of creating a new
 * one, so the learner sees the card they just saved.
 *
 * <p>Not {@code @Transactional} on purpose: when two requests race, the loser's insert breaks the partial unique
 * index and its transaction can no longer be used, so the existing card is read back in a fresh one.
 */
@Service
@RequiredArgsConstructor
public class SavePracticeQuestionFlashcardUseCase {
    private final FlashcardRepository repository;

    public SavedFlashcard execute(UUID userId, UUID practiceQuestionId, String front, String back) {
        var existing = repository.findLivePracticeQuestionCard(userId, practiceQuestionId);
        if (existing.isPresent()) {
            return new SavedFlashcard(FlashcardResult.from(restoreIfArchived(existing.get())), false);
        }
        Flashcard card = Flashcard.create(userId, FlashcardSourceType.PRACTICE_QUESTION, null, practiceQuestionId,
                null, front, back);
        try {
            return new SavedFlashcard(FlashcardResult.from(repository.save(card)), true);
        } catch (ConflictException conflict) {
            return repository.findLivePracticeQuestionCard(userId, practiceQuestionId)
                    .map(saved -> new SavedFlashcard(FlashcardResult.from(restoreIfArchived(saved)), false))
                    .orElseThrow(() -> conflict);
        }
    }

    /** Front, back and source stay as first saved; only the status changes. */
    private Flashcard restoreIfArchived(Flashcard card) {
        if (card.getStatus() != LibraryStatus.ARCHIVED) {
            return card;
        }
        card.update(card.getSourceType(), card.getVocabularySenseId(), card.getSourceReferenceId(),
                card.getHighlightedText(), card.getFront(), card.getBack(), LibraryStatus.ACTIVE);
        return repository.save(card);
    }
}
