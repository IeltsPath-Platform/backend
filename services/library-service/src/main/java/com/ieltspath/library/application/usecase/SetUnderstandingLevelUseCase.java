package com.ieltspath.library.application.usecase;

import com.ieltspath.library.config.FlashcardSrsProperties;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SetUnderstandingLevelUseCase {
    private final FlashcardRepository cards;
    private final FlashcardSrsProperties srs;
    private final Clock clock;
    @Transactional
    public void execute(UUID userId, UUID cardId, UnderstandingLevel level, UUID requestId) {
        var card = cards.findForReviewByIdAndUserId(cardId, userId).orElseThrow(ResourceNotFoundException::new);
        if (card.getStatus() != LibraryStatus.ACTIVE || card.getSourceType() != FlashcardSourceType.VOCABULARY_SENSE)
            throw new ResourceNotFoundException();
        if (card.setUnderstanding(level, clock.instant(), srs.getIntervalsDays(), requestId)) cards.save(card);
    }
}
