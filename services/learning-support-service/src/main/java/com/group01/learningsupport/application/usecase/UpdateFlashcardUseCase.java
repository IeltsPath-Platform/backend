package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.result.FlashcardResult;
import com.group01.learningsupport.domain.aggregate.Flashcard;
import com.group01.learningsupport.domain.repository.FlashcardRepository;
import com.group01.learningsupport.domain.vo.FlashcardSourceType;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateFlashcardUseCase {
    private final FlashcardRepository repository;

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
        Flashcard card = ApplicationSupport.required(repository.findAvailableByIdAndUserId(flashcardId, userId));
        card.update(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, status);
        return FlashcardResult.from(repository.save(card));
    }
}
