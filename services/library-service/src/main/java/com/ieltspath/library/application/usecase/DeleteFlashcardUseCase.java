package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteFlashcardUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeleteFlashcardUseCase.class);

    private final FlashcardRepository repository;
    private final FlashcardDeckItemRepository items;
    private final FlashcardImageStorage storage;

    @Transactional
    public void execute(UUID userId, UUID flashcardId) {
        Flashcard card = ApplicationSupport.required(repository.findByIdAndUserId(flashcardId, userId));
        String oldKey = card.getImageKey();
        card.update(
                card.getSourceType(),
                card.getVocabularySenseId(),
                card.getSourceReferenceId(),
                card.getHighlightedText(),
                card.getFront(),
                card.getBack(),
                LibraryStatus.DELETED
        );
        repository.save(card);
        items.deleteByFlashcardId(flashcardId);

        if (oldKey != null) {
            deleteAfterCommit(oldKey);
        }
    }

    private void deleteAfterCommit(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        storage.delete(key);
                    } catch (Exception e) {
                        log.error("Failed to delete card image after commit: {}", e.getClass().getSimpleName());
                    }
                }
            });
        } else {
            try {
                storage.delete(key);
            } catch (Exception e) {
                log.error("Failed to delete card image: {}", e.getClass().getSimpleName());
            }
        }
    }
}
