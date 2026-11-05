package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.ErrorTrapCatalog.ErrorTraps;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.ReviewRule.ReviewCandidate;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErrorTrapReviewsTest {
    private final UUID user = UUID.randomUUID();
    private final UUID lesson = UUID.randomUUID();
    private final UUID parent = UUID.randomUUID();
    private final UUID trap = UUID.randomUUID();
    private final UUID assigned = UUID.randomUUID();
    private final ReviewItemRepository reviews = mock(ReviewItemRepository.class);
    private final MasteryProfileRepository profiles = mock(MasteryProfileRepository.class);
    private final PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
    private final KnowledgeEvidenceRepository evidence = mock(KnowledgeEvidenceRepository.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final CompletedLessonLocator lessonLocator = mock(CompletedLessonLocator.class);
    private final ErrorTrapCatalog catalog = mock(ErrorTrapCatalog.class);
    private ErrorTrapReviews subject;

    @BeforeEach
    void setUp() {
        subject = new ErrorTrapReviews(reviews, profiles, attempts, evidence, content, lessonLocator, catalog, 1);
        when(catalog.load()).thenReturn(new ErrorTraps(Map.of("TFNG_NG_AS_FALSE", trap), Map.of(trap, parent),
                Map.of(parent, List.of(trap))));
        when(profiles.findErrorTrapProfiles(user)).thenReturn(List.of(profile(trap, .2, MasteryTier.PRIORITY)));
        when(reviews.findPending(user)).thenReturn(List.of());
        when(attempts.revealedPackageIds(user)).thenReturn(java.util.Set.of());
        when(reviews.assignedPackageIds(user)).thenReturn(List.of(assigned));
        when(lessonLocator.teaching(user, parent)).thenReturn(Optional.of(lesson));
        when(content.searchPracticeSets(eq(parent), anyList(), eq(3), eq(1), eq(lesson), eq("TFNG_NG_AS_FALSE")))
                .thenReturn(List.of(new LearningContentClient.PracticeSet(UUID.randomUUID(), UUID.randomUUID(),
                        "TRAP_SET", 3, 3, null, null, 2)));
    }

    @Test
    void createsOptionalErrorPatternReviewForParentWhenTrapIsPriorityAndSetHasEnoughTrapQuestions() {
        subject.create(user, null, List.of(trap));

        verify(reviews).insertPending(user, List.of(new ReviewCandidate(parent, lesson, null)), false, "ERROR_PATTERN");
        verify(content).searchPracticeSets(eq(parent), anyList(), eq(3), eq(1), eq(lesson), eq("TFNG_NG_AS_FALSE"));
    }

    @Test
    void skipsWhenParentAlreadyHasPendingReview() {
        when(reviews.findPending(user)).thenReturn(List.of(pending(parent)));
        subject.create(user, null, List.of(trap));
        verify(reviews, never()).insertPending(any(), anyList(), anyBoolean(), anyString());
    }

    @Test
    void skipsWhenNoSetHasAtLeastTwoTrapQuestions() {
        when(content.searchPracticeSets(eq(parent), anyList(), eq(3), eq(1), eq(lesson), eq("TFNG_NG_AS_FALSE")))
                .thenReturn(List.of(new LearningContentClient.PracticeSet(UUID.randomUUID(), UUID.randomUUID(),
                        "NO_TRAP_SET", 3, 3, null, null, 1)));
        subject.create(user, null, List.of(trap));
        verify(reviews, never()).insertPending(any(), anyList(), anyBoolean(), anyString());
    }

    @Test
    void ignoresContentFailureSoSubmissionCanComplete() {
        when(content.searchPracticeSets(eq(parent), anyList(), eq(3), eq(1), eq(lesson), eq("TFNG_NG_AS_FALSE")))
                .thenThrow(new IllegalStateException("content unavailable"));
        subject.create(user, null, List.of(trap));
        verify(reviews, never()).insertPending(any(), anyList(), anyBoolean(), anyString());
    }

    @Test
    void skipsNonPriorityChildrenAndParentsWithoutACompletedLesson() {
        when(profiles.findErrorTrapProfiles(user)).thenReturn(List.of(profile(trap, .4, MasteryTier.CONSOLIDATE)));
        subject.create(user, null, List.of(trap));
        verify(lessonLocator, never()).teaching(user, parent);
        verify(reviews, never()).insertPending(any(), anyList(), anyBoolean(), anyString());

        when(profiles.findErrorTrapProfiles(user)).thenReturn(List.of(profile(trap, .2, MasteryTier.PRIORITY)));
        when(lessonLocator.teaching(user, parent)).thenReturn(Optional.empty());
        subject.create(user, null, List.of(trap));
        verify(reviews, never()).insertPending(any(), anyList(), anyBoolean(), anyString());
    }

    @Test
    void usesTheSubmissionLessonWhenItTeachesTheParent() {
        LearningContentClient.Lesson source = new LearningContentClient.Lesson(lesson, UUID.randomUUID(), "L1", "Lesson",
                null, 1, List.of(parent), List.of());
        when(content.getLesson(lesson)).thenReturn(source);

        subject.create(user, lesson, List.of(trap));

        verify(lessonLocator, never()).teaching(user, parent);
        verify(reviews).insertPending(user, List.of(new ReviewCandidate(parent, lesson, null)), false, "ERROR_PATTERN");
    }

    @Test
    void honorsThePerSubmissionReviewLimitInWeakestFirstOrder() {
        UUID secondParent = UUID.randomUUID();
        UUID secondTrap = UUID.randomUUID();
        when(catalog.load()).thenReturn(new ErrorTraps(Map.of("A_ERROR", trap, "B_ERROR", secondTrap),
                Map.of(trap, parent, secondTrap, secondParent), Map.of(parent, List.of(trap),
                secondParent, List.of(secondTrap))));
        when(profiles.findErrorTrapProfiles(user)).thenReturn(List.of(
                profile(trap, .3, MasteryTier.PRIORITY), profile(secondTrap, .1, MasteryTier.PRIORITY)));
        when(lessonLocator.teaching(user, secondParent)).thenReturn(Optional.of(UUID.randomUUID()));
        when(content.searchPracticeSets(any(), anyList(), eq(3), eq(1), any(), any()))
                .thenReturn(List.of(new LearningContentClient.PracticeSet(UUID.randomUUID(), UUID.randomUUID(),
                        "TRAP_SET", 3, 3, null, null, 2)));

        subject.create(user, null, List.of(trap, secondTrap));

        verify(reviews, times(1)).insertPending(eq(user), anyList(), eq(false), eq("ERROR_PATTERN"));
        verify(content, times(1)).searchPracticeSets(eq(secondParent), anyList(), eq(3), eq(1),
                any(), eq("B_ERROR"));
    }

    private MasteryProfile profile(UUID kp, double mastery, MasteryTier tier) {
        return new MasteryProfile(user, kp, mastery, tier, 4, 0, Instant.EPOCH, null, null, List.of());
    }

    private PendingReview pending(UUID kp) {
        return new PendingReview(UUID.randomUUID(), lesson, kp, null, true, "PRACTICE");
    }
}
