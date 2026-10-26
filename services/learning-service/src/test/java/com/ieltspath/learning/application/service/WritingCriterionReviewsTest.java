package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.ReviewRule.ReviewCandidate;
import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WritingCriterionReviewsTest {
    private final ReviewItemRepository reviews = mock(ReviewItemRepository.class);
    private final PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final WritingCriterionReviews service = new WritingCriterionReviews(reviews, attempts, content, 2);
    private final UUID userId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();

    @Test void batchesAvailabilityAndInsertsTheTwoWeakestOptionalReviews() {
        UUID lr = UUID.randomUUID(), gra = UUID.randomUUID(), cc = UUID.randomUUID();
        UUID revealed = UUID.randomUUID(), assigned = UUID.randomUUID();
        when(reviews.findPending(userId)).thenReturn(List.of());
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of(revealed));
        when(reviews.assignedPackageIds(userId)).thenReturn(List.of(assigned));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(lr, 3, gra, 3, cc, 1));

        service.create(userId, lessonId, List.of(failed(lr, "LR", "5.0"), failed(gra, "GRA", "4.5"), failed(cc, "CC", "5.0")));

        verify(content, times(1)).practiceSetAvailability(argThat(ids -> Set.copyOf(ids).equals(Set.of(lr, gra, cc))),
                argThat(ids -> Set.copyOf(ids).equals(Set.of(revealed, assigned))), eq(3));
        verify(reviews).insertPending(userId, List.of(new ReviewCandidate(gra, lessonId, LearningSkill.WRITING),
                new ReviewCandidate(cc, lessonId, LearningSkill.WRITING)), false, "WRITING_CRITERION");
    }

    @Test void existingPendingPointDoesNotRequestAvailabilityOrInsertAgain() {
        UUID kp = UUID.randomUUID();
        when(reviews.findPending(userId)).thenReturn(List.of(new PendingReview(UUID.randomUUID(), lessonId, kp,
                LearningSkill.WRITING, false, "WRITING_CRITERION")));
        service.create(userId, lessonId, List.of(failed(kp, "LR", "5.0")));
        verifyNoInteractions(content, attempts);
        verify(reviews, never()).insertPending(any(), anyList(), anyBoolean(), anyString());
    }

    @Test void freeWritingAndNoFailedCriteriaDoNotCreateReviews() {
        service.create(userId, null, List.of(failed(UUID.randomUUID(), "LR", "5.0")));
        service.create(userId, lessonId, List.of());
        verifyNoInteractions(content, reviews, attempts);
    }

    private static FailedWritingCriterion failed(UUID kp, String code, String band) {
        return new FailedWritingCriterion(kp, code, new BigDecimal(band));
    }
}
