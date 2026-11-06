package com.ieltspath.learning.application.service;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.ReviewRule.ReviewCandidate;
import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private final WritingCriterionReviews service = new WritingCriterionReviews(reviews, attempts, content, 2, 2);
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
        verify(reviews).insertCriterionPending(userId, lessonId, LearningSkill.WRITING, "WRITING_CRITERION",
                List.of(failed(gra, "GRA", "4.5"), failed(cc, "CC", "5.0")));
    }

    @Test void existingPendingPointDoesNotRequestAvailabilityOrInsertAgain() {
        UUID kp = UUID.randomUUID();
        when(reviews.findPending(userId)).thenReturn(List.of(new PendingReview(UUID.randomUUID(), lessonId, kp,
                LearningSkill.WRITING, false, "WRITING_CRITERION")));
        service.create(userId, lessonId, List.of(failed(kp, "LR", "5.0")));
        verifyNoInteractions(content, attempts);
        verify(reviews, never()).insertCriterionPending(any(), any(), any(), anyString(), any());
    }

    @Test void freeWritingAndNoFailedCriteriaDoNotCreateReviews() {
        service.create(userId, null, List.of(failed(UUID.randomUUID(), "LR", "5.0")));
        service.create(userId, lessonId, List.of());
        verifyNoInteractions(content, reviews, attempts);
    }

    @Test void aWeakSpeakingCriterionGetsAnOptionalSpeakingReviewUnderItsOwnTrigger() {
        UUID fc = UUID.randomUUID(), pr = UUID.randomUUID(), lr = UUID.randomUUID();
        when(reviews.findPending(userId)).thenReturn(List.of());
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of());
        when(reviews.assignedPackageIds(userId)).thenReturn(List.of());
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(fc, 3, pr, 3, lr, 3));

        service.createForSpeaking(userId, lessonId, List.of(failed(fc, "FC", "4.5"), failed(pr, "PR", "4.0"), failed(lr, "LR", "5.0")));

        verify(reviews).insertCriterionPending(userId, lessonId, LearningSkill.SPEAKING, "SPEAKING_CRITERION",
                List.of(failed(pr, "PR", "4.0"), failed(fc, "FC", "4.5")));
    }

    @Test void aSpeakingCriterionWithNoUnseenSetOrWithAPendingReviewGetsNoReview() {
        UUID noSets = UUID.randomUUID(), waiting = UUID.randomUUID();
        when(reviews.findPending(userId)).thenReturn(List.of(new PendingReview(UUID.randomUUID(), lessonId, waiting,
                LearningSkill.SPEAKING, false, "SPEAKING_CRITERION")));
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of());
        when(reviews.assignedPackageIds(userId)).thenReturn(List.of());
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(noSets, 0));

        service.createForSpeaking(userId, lessonId, List.of(failed(noSets, "FC", "4.0"), failed(waiting, "LR", "4.0")));

        verify(reviews).insertCriterionPending(userId, lessonId, LearningSkill.SPEAKING, "SPEAKING_CRITERION", List.of());
    }

    @Test void speakingHasItsOwnLimitSeparateFromWriting() {
        UUID fc = UUID.randomUUID(), lr = UUID.randomUUID();
        when(reviews.findPending(userId)).thenReturn(List.of());
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of());
        when(reviews.assignedPackageIds(userId)).thenReturn(List.of());
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(fc, 3, lr, 3));

        new WritingCriterionReviews(reviews, attempts, content, 2, 1)
                .createForSpeaking(userId, lessonId, List.of(failed(fc, "FC", "4.5"), failed(lr, "LR", "5.0")));

        verify(reviews).insertCriterionPending(userId, lessonId, LearningSkill.SPEAKING, "SPEAKING_CRITERION",
                List.of(failed(fc, "FC", "4.5")));
    }

    @Test void aNegativeSpeakingLimitIsRefused() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new WritingCriterionReviews(reviews, attempts, content, 2, -1));
    }

    @Test void speakingWithoutALessonOrWithoutWeakCriteriaCreatesNothing() {
        service.createForSpeaking(userId, null, List.of(failed(UUID.randomUUID(), "FC", "4.0")));
        service.createForSpeaking(userId, lessonId, List.of());
        verifyNoInteractions(content, reviews, attempts);
    }

    @Test void theEssayWhoseGradeFoundACriterionWeakIsKeptOnItsReview() {
        UUID kp = UUID.randomUUID(), essay = UUID.randomUUID();
        when(reviews.findPending(userId)).thenReturn(List.of());
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of());
        when(reviews.assignedPackageIds(userId)).thenReturn(List.of());
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(kp, 3));
        var weak = failed(kp, "LR", "5.0").fromSubmission(essay);

        service.create(userId, lessonId, List.of(weak));

        verify(reviews).insertCriterionPending(userId, lessonId, LearningSkill.WRITING, "WRITING_CRITERION", List.of(weak));
    }

    private static FailedWritingCriterion failed(UUID kp, String code, String band) {
        return new FailedWritingCriterion(kp, code, new BigDecimal(band));
    }

    @Test void availabilityIsReadOnceBeforeTheLockAndCreationMakesNoContentCall() {
        UUID lr = UUID.randomUUID(), gra = UUID.randomUUID();
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of());
        when(reviews.assignedPackageIds(userId)).thenReturn(List.of());
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(lr, 3, gra, 3));
        var available = service.availabilityOf(userId, List.of(failed(lr, "LR", "5.0"), failed(gra, "GRA", "4.5")));
        verify(content, times(1)).practiceSetAvailability(anyList(), anyList(), eq(3));

        service.createWithAvailability(userId, lessonId, List.of(failed(lr, "LR", "5.0"), failed(gra, "GRA", "4.5")), 1, available);

        verify(content, times(1)).practiceSetAvailability(anyList(), anyList(), eq(3));
        verify(reviews).insertCriterionPending(userId, lessonId, LearningSkill.WRITING, "WRITING_CRITERION",
                List.of(failed(gra, "GRA", "4.5")));
    }

    @Test void aCriterionWithNoUnseenSetIsNotSelectedWhenAvailabilityIsKnown() {
        UUID kp = UUID.randomUUID();
        when(reviews.findPending(userId)).thenReturn(List.of());

        service.createWithAvailability(userId, lessonId, List.of(failed(kp, "LR", "4.0")), 2, Map.of(kp, 0));

        verify(reviews).insertCriterionPending(userId, lessonId, LearningSkill.WRITING, "WRITING_CRITERION", List.of());
    }

    @Test void noAvailabilityIsReadWhenNothingFailed() {
        assertEquals(Map.of(), service.availabilityOf(userId, List.of()));
        verifyNoInteractions(content);
    }
}
