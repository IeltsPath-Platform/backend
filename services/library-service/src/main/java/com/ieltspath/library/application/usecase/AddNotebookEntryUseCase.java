package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.NotebookEntryResult;
import com.ieltspath.library.application.port.VocabularyNotebookReader;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.exception.ConflictException;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.VocabularySenseCard;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddNotebookEntryUseCase {
    private final FlashcardRepository cards;
    private final VocabularyRepository vocabulary;
    private final VocabularyNotebookReader notebookReader;
    private final TransactionTemplate transactions;

    public SavedNotebookEntry execute(UUID userId, UUID senseId) {
        VocabularySenseCard sense = vocabulary.findActiveSensesByIds(List.of(senseId)).stream().findFirst()
                .orElseThrow(ResourceNotFoundException::new);
        Flashcard existing = cards.findLiveVocabularySenseCard(userId, senseId).orElse(null);
        if (existing != null) return result(userId, restoreIfArchived(existing), false);
        Flashcard created = Flashcard.create(userId, FlashcardSourceType.VOCABULARY_SENSE, senseId,
                senseId, null, sense.lemma(), nonblank(sense.vietnameseMeaning(), sense.englishDefinition()),
                null, sense.exampleSentence());
        try {
            return result(userId, transactions.execute(status -> cards.save(created)), true);
        } catch (DataIntegrityViolationException | ConflictException race) {
            return cards.findLiveVocabularySenseCard(userId, senseId)
                    .map(card -> result(userId, restoreIfArchived(card), false)).orElseThrow(() -> race);
        }
    }

    private Flashcard restoreIfArchived(Flashcard card) {
        if (card.getStatus() != com.ieltspath.library.domain.vo.LibraryStatus.ARCHIVED) return card;
        card.update(card.getSourceType(), card.getVocabularySenseId(), card.getSourceReferenceId(), card.getHighlightedText(),
                card.getFront(), card.getBack(), com.ieltspath.library.domain.vo.LibraryStatus.ACTIVE,
                card.getImageKey(), card.getExampleSentence());
        return cards.save(card);
    }

    private SavedNotebookEntry result(UUID userId, Flashcard card, boolean created) {
        NotebookEntryResult entry = notebookReader.findEntry(userId, card.getId())
                .orElseThrow(ResourceNotFoundException::new);
        return new SavedNotebookEntry(entry, created);
    }

    private String nonblank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }

    public record SavedNotebookEntry(NotebookEntryResult entry, boolean created) {}
}
