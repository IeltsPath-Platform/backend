package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.result.ReviewBasketSummaryResult;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class GetReviewBasketSummaryUseCaseTest {
    final MasteryProfileRepository profiles = mock(MasteryProfileRepository.class);
    final ReviewItemRepository reviews = mock(ReviewItemRepository.class);
    final GetReviewBasketSummaryUseCase summary = new GetReviewBasketSummaryUseCase(profiles, reviews);
    final UUID learner = UUID.randomUUID();
    final UUID lesson = UUID.randomUUID();

    private PendingReview review(UUID point, boolean blocking) {
        return new PendingReview(UUID.randomUUID(), lesson, point, LearningSkill.READING, blocking, "PRACTICE");
    }

    @Test
    void countsEachBlockingReviewAndTheDuePointsWithoutOne() {
        UUID blockedPoint = UUID.randomUUID();
        UUID otherBlocked = UUID.randomUUID();
        when(reviews.findPending(learner)).thenReturn(List.of(review(blockedPoint, true), review(otherBlocked, true)));
        when(profiles.countDue(eq(learner), any(Instant.class), eq(Set.of(blockedPoint, otherBlocked)))).thenReturn(3);

        ReviewBasketSummaryResult result = summary.execute(learner);

        assertEquals(2, result.required());
        assertEquals(3, result.due());
    }

    @Test
    void anOptionalReviewIsNotRequiredAndDoesNotHideItsDuePoint() {
        UUID optionalPoint = UUID.randomUUID();
        when(reviews.findPending(learner)).thenReturn(List.of(review(optionalPoint, false)));
        when(profiles.countDue(eq(learner), any(Instant.class), eq(Set.of()))).thenReturn(1);

        ReviewBasketSummaryResult result = summary.execute(learner);

        assertEquals(0, result.required());
        assertEquals(1, result.due());
    }

    @Test
    void aLearnerWithNothingPendingHasAnEmptySummary() {
        when(reviews.findPending(learner)).thenReturn(List.of());
        when(profiles.countDue(eq(learner), any(Instant.class), eq(Set.of()))).thenReturn(0);

        ReviewBasketSummaryResult result = summary.execute(learner);

        assertEquals(new ReviewBasketSummaryResult(0, 0), result);
        verify(reviews).findPending(learner);
        verifyNoMoreInteractions(reviews);
    }
}
