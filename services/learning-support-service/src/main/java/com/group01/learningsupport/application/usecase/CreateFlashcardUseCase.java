package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.result.FlashcardResult;
import com.group01.learningsupport.domain.aggregate.Flashcard;
import com.group01.learningsupport.domain.repository.FlashcardRepository;
import com.group01.learningsupport.domain.vo.FlashcardSourceType;
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
