package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateFlashcardUseCase {
    private final FlashcardRepository repository;

    @Transactional
    public FlashcardResult execute(
            UUID userId,
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back
    ) {
        return FlashcardResult.from(repository.save(Flashcard.create(
                userId, sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back
        )));
    }
}
