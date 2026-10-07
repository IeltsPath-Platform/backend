package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateFlashcardUseCase {
    private final FlashcardRepository repository;
    private final FlashcardDeckItemRepository items;

    @Transactional
    public FlashcardResult execute(
            UUID userId,
            UUID flashcardId,
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back,
            LibraryStatus status
    ) {
        Flashcard card = ApplicationSupport.required(repository.findByIdAndUserId(flashcardId, userId));
        card.update(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, status);
        if (status != LibraryStatus.ACTIVE) {
            items.deleteByFlashcardId(flashcardId);
        }
        return FlashcardResult.from(repository.save(card));
    }
}
