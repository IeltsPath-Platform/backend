package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.service.FlashcardImageUsageService;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteFlashcardUseCase {
    private final FlashcardRepository repository;
    private final FlashcardDeckItemRepository items;
    private final FlashcardImageStorage storage;
    private final FlashcardImageUsageService imageUsage;
    private final TransactionTemplate transactionTemplate;

    public void execute(UUID userId, UUID flashcardId) {
        Flashcard card = ApplicationSupport.required(repository.findByIdAndUserId(flashcardId, userId));
        String oldKey = card.getImageKey();

        transactionTemplate.executeWithoutResult(status -> {
            card.update(
                    card.getSourceType(),
                    card.getVocabularySenseId(),
                    card.getSourceReferenceId(),
                    card.getHighlightedText(),
                    card.getFront(),
                    card.getBack(),
                    LibraryStatus.DELETED,
                    card.getImageKey(),
                    card.getExampleSentence()
            );
            repository.save(card);
            items.deleteByFlashcardId(flashcardId);
        });

        if (oldKey != null && !imageUsage.isStillReferenced(oldKey)) {
            storage.deleteAfterCommit(oldKey);
        }
    }
}
