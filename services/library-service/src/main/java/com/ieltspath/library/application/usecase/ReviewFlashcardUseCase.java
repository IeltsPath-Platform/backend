package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.port.FlashcardVocabularyMetadataReader;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.config.FlashcardSrsProperties;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewFlashcardUseCase {
    private final FlashcardRepository repository;
    private final FlashcardSrsProperties srsProperties;
    private final FlashcardVocabularyMetadataReader vocabularyMetadataReader;
    private final FlashcardImageStorage storage;

    @Transactional
    public FlashcardResult execute(UUID userId, UUID flashcardId, boolean correct, UUID requestId) {
        Flashcard card = repository.findForReviewByIdAndUserId(flashcardId, userId)
                .orElseThrow(ResourceNotFoundException::new);
        if (card.getStatus() != LibraryStatus.ACTIVE) {
            throw new ResourceNotFoundException();
        }
        if (card.review(correct, requestId, Instant.now(), srsProperties.getIntervalsDays())) {
            card = repository.save(card);
        }
        var metadata = card.getVocabularySenseId() == null
                ? null
                : vocabularyMetadataReader.findBySenseIds(List.of(card.getVocabularySenseId()))
                        .get(card.getVocabularySenseId());
        return FlashcardResult.from(card, storage.publicUrl(card.getImageKey())).withVocabularyMetadata(metadata);
    }
}
