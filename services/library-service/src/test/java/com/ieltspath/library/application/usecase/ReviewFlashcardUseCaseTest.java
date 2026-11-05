package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.port.FlashcardReviewEventWriter;
import com.ieltspath.library.application.port.FlashcardVocabularyMetadataReader;
import com.ieltspath.library.config.FlashcardSrsProperties;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewFlashcardUseCaseTest {
    @Mock FlashcardRepository repository;
    @Mock FlashcardVocabularyMetadataReader metadataReader;
    @Mock FlashcardImageStorage storage;
    @Mock FlashcardReviewEventWriter eventWriter;
    private ReviewFlashcardUseCase useCase;
    private final UUID userId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();
    private final UUID senseId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();

    @BeforeEach void setUp() {
        useCase = new ReviewFlashcardUseCase(repository, new FlashcardSrsProperties(), metadataReader, storage, eventWriter);
        when(metadataReader.findBySenseIds(anyList())).thenReturn(Map.of());
        when(storage.publicUrl(any())).thenReturn(null);
    }

    @Test void recordsAppliedReviewWithOptionalSessionId() {
        UUID sessionId = UUID.randomUUID();
        Flashcard card = card(null);
        when(repository.findForReviewByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(repository.save(card)).thenReturn(card);

        useCase.execute(userId, cardId, true, requestId, sessionId);

        verify(eventWriter).append(eq(userId), eq(cardId), eq(senseId), eq(sessionId), eq(true), eq(card.getLastReviewedAt()));
    }

    @Test void recordsAppliedReviewWhenSessionIdIsNull() {
        Flashcard card = card(null);
        when(repository.findForReviewByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(repository.save(card)).thenReturn(card);

        useCase.execute(userId, cardId, false, requestId, null);

        verify(eventWriter).append(eq(userId), eq(cardId), eq(senseId), isNull(), eq(false), eq(card.getLastReviewedAt()));
    }

    @Test void replayedRequestDoesNotSaveOrAppendAnEvent() {
        Flashcard card = card(requestId);
        when(repository.findForReviewByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));

        useCase.execute(userId, cardId, true, requestId, UUID.randomUUID());

        verify(repository, never()).save(any());
        verifyNoInteractions(eventWriter);
    }

    private Flashcard card(UUID lastRequestId) {
        return Flashcard.builder().id(cardId).userId(userId).sourceType(FlashcardSourceType.VOCABULARY_SENSE)
                .vocabularySenseId(senseId).front("front").back("back").status(LibraryStatus.ACTIVE)
                .createdAt(Instant.parse("2026-10-01T00:00:00Z")).updatedAt(Instant.parse("2026-10-01T00:00:00Z"))
                .leitnerBox(1).reviewCount(0).lastReviewRequestId(lastRequestId).imageKey("images/card.png").build();
    }
}
