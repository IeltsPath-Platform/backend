package com.group01.learning.application.usecase;

import com.group01.learning.application.command.AssessmentResult;
import com.group01.learning.application.port.AssessmentResultLog;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.service.ReviewReevaluation;
import com.group01.learning.domain.aggregate.LearnerPlacement;
import com.group01.learning.domain.repository.*;
import com.group01.learning.domain.vo.BandLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApplyPlacementResultUseCaseTest {
    private final LearnerLock lock = mock(LearnerLock.class);
    private final KnowledgeEvidenceRepository evidence = mock(KnowledgeEvidenceRepository.class);
    private final LearnerCurriculumRepository curricula = mock(LearnerCurriculumRepository.class);
    private final AssessmentResultLog results = mock(AssessmentResultLog.class);
    private final TopicTestAssignmentRepository assignments = mock(TopicTestAssignmentRepository.class);
    private final ReviewReevaluation reviews = mock(ReviewReevaluation.class);
    private final LearnerPlacementRepository placements = mock(LearnerPlacementRepository.class);
    private final ApplyAssessmentResultUseCase useCase = new ApplyAssessmentResultUseCase(
            lock, evidence, curricula, results, assignments, reviews, placements);
    private final UUID user = UUID.randomUUID();
    private final Instant completed = Instant.parse("2026-10-06T10:00:00Z");

    @Test
    void storesPlacementAfterLockAndVersionWithoutLearningEvidenceOrGates() {
        AssessmentResult event = event(UUID.randomUUID(), 1, "6.0", completed);
        useCase.execute(event);
        var order = inOrder(lock, results, placements);
        order.verify(lock).lock(user);
        order.verify(results).appliedVersion(user, event.attemptId());
        order.verify(results).recordVersion(user, event.attemptId(), 1);
        order.verify(placements).find(user);
        order.verify(placements).save(any());
        var saved = org.mockito.ArgumentCaptor.forClass(LearnerPlacement.class);
        verify(placements).save(saved.capture());
        assertThat(saved.getValue().band().value()).isEqualTo(new BigDecimal("6.0"));
        verifyNoInteractions(evidence, curricula, assignments, reviews);
    }

    @Test
    void oldAttemptDoesNotSaveAndCurrentAttemptRegradeSavesNewBand() {
        UUID current = UUID.randomUUID();
        LearnerPlacement placement = LearnerPlacement.create(user, new BandLevel(new BigDecimal("6.5")),
                current, completed);
        when(placements.find(user)).thenReturn(Optional.of(placement));
        useCase.execute(event(UUID.randomUUID(), 1, "4.0", completed.minusSeconds(1)));
        verify(placements, never()).save(any());
        when(results.appliedVersion(user, current)).thenReturn(Optional.of(1));
        useCase.execute(event(current, 2, "7.0", completed));
        assertThat(placement.band().value()).isEqualTo(new BigDecimal("7.0"));
        verify(placements).save(placement);
        verify(evidence, never()).append(any(), any());
        verifyNoInteractions(curricula, assignments, reviews);
    }

    @Test
    void missingBandAndAppliedVersionDoNotTouchPlacementState() {
        useCase.execute(event(UUID.randomUUID(), 1, null, completed));
        AssessmentResult replay = event(UUID.randomUUID(), 2, "6.0", completed);
        when(results.appliedVersion(user, replay.attemptId())).thenReturn(Optional.of(2));
        useCase.execute(replay);
        verifyNoInteractions(placements, evidence, curricula, assignments, reviews);
        verify(results, never()).recordVersion(user, replay.attemptId(), 2);
    }

    private AssessmentResult event(UUID attempt, int version, String band, Instant completedAt) {
        return new AssessmentResult(UUID.randomUUID(), user, null, attempt, UUID.randomUUID(), version,
                "PLACEMENT", completedAt, List.of(), band == null ? null : new BigDecimal(band));
    }
}
