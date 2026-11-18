package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.domain.aggregate.PlacementAssignment;
import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ReleasePlacementAssignmentUseCaseTest {
    private final UUID user = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final PlacementAssignmentRepository assignments = mock(PlacementAssignmentRepository.class);
    private final LearnerLock lock = mock(LearnerLock.class);

    @Test
    void releasesOnlyTheCallersOpenAssignment() {
        var assignment = PlacementAssignment.assign(id, user, UUID.randomUUID(), UUID.randomUUID(),
                PlacementPool.BAND_3_5, now.minusSeconds(20));
        when(assignments.findOwnedOpen(user, id)).thenReturn(Optional.of(assignment));
        new ReleasePlacementAssignmentUseCase(assignments, lock, ClockFixed.at(now)).execute(user, id);

        assertThat(assignment.isReleased()).isTrue();
        assertThat(assignment.releasedAt()).isEqualTo(now);
        verify(assignments).save(assignment);
    }

    @Test
    void anotherLearnersOrCompletedAssignmentCannotBeReleased() {
        when(assignments.findOwnedOpen(user, id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ReleasePlacementAssignmentUseCase(assignments, lock, ClockFixed.at(now))
                .execute(user, id)).isInstanceOfSatisfying(LearningRequestException.class,
                error -> assertThat(error.getStatus()).isEqualTo(404));
        verify(assignments, never()).save(any());
    }

    private static final class ClockFixed {
        static java.time.Clock at(Instant instant) {
            return java.time.Clock.fixed(instant, java.time.ZoneOffset.UTC);
        }
    }
}
