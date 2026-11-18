package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.PlacementPicker;
import com.ieltspath.learning.application.service.PlacementCatalog;
import com.ieltspath.learning.domain.aggregate.PlacementAssignment;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssignPlacementTestUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private final UUID userId = UUID.randomUUID();
    private final PlacementAssignmentRepository assignments = mock(PlacementAssignmentRepository.class);
    private final LearnerPlacementRepository placements = mock(LearnerPlacementRepository.class);
    private final LearnerLock lock = mock(LearnerLock.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final PlacementPicker picker = bound -> 0;
    private final CreatePlacementAssignment creator = new CreatePlacementAssignment(assignments, lock, picker);
    private AssignPlacementTestUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AssignPlacementTestUseCase(assignments, new PlacementCatalog(content, clock, 30), creator,
                clock, 3, 15);
        when(assignments.countSince(eq(userId), any())).thenReturn(0);
        when(assignments.findReusable(eq(userId), eq(PlacementPool.BAND_3_5), any())).thenReturn(Optional.empty());
        when(assignments.latestByPackage(userId)).thenReturn(Map.of());
        when(placements.find(userId)).thenReturn(Optional.empty());
    }

    @Test
    void picksAnUnusedPublishedPackageFromTheRequestedPoolAndSavesIt() {
        var first = packageIn(PlacementPool.BAND_3_5);
        var otherPool = packageIn(PlacementPool.BAND_0_3);
        when(content.getPlacementPackages(PlacementPool.BAND_3_5)).thenReturn(List.of(first, otherPool));

        var result = useCase.execute(userId, PlacementPool.BAND_3_5);

        assertThat(result.pool()).isEqualTo(PlacementPool.BAND_3_5);
        assertThat(result.packageId()).isEqualTo(first.packageId());
        assertThat(result.packageVersionId()).isEqualTo(first.packageVersionId());
        verify(assignments).save(any(PlacementAssignment.class));
        verify(lock).lock(userId);
    }

    @Test
    void picksOldestPackageWhenEveryPublishedPackageHasBeenAssigned() {
        var first = packageIn(PlacementPool.BAND_3_5);
        var second = packageIn(PlacementPool.BAND_3_5);
        when(content.getPlacementPackages(PlacementPool.BAND_3_5)).thenReturn(List.of(first, second));
        when(assignments.latestByPackage(userId)).thenReturn(Map.of(first.packageId(), NOW.minusSeconds(100),
                second.packageId(), NOW.minusSeconds(50)));
        var rotationCreator = new CreatePlacementAssignment(assignments, lock, bound -> bound - 1);
        useCase = new AssignPlacementTestUseCase(assignments, new PlacementCatalog(content, clock, 30),
                rotationCreator, clock, 3, 15);

        var result = useCase.execute(userId, PlacementPool.BAND_3_5);

        assertThat(result.packageId()).isEqualTo(first.packageId());
    }

    @Test
    void returnsReusableAssignmentWithoutCallingContentOrTakingLock() {
        var existing = PlacementAssignment.assign(UUID.randomUUID(), userId, packageIn(PlacementPool.BAND_3_5).packageId(),
                UUID.randomUUID(), PlacementPool.BAND_3_5, NOW.minusSeconds(60));
        when(assignments.findReusable(userId, PlacementPool.BAND_3_5, NOW.minusSeconds(900)))
                .thenReturn(Optional.of(existing));

        var result = useCase.execute(userId, PlacementPool.BAND_3_5);

        assertThat(result.assignmentId()).isEqualTo(existing.id());
        verifyNoInteractions(content, lock);
    }

    @Test
    void rejectsFourthCountedAssignmentBeforeCallingContent() {
        when(assignments.countSince(userId, NOW.minusSeconds(24 * 60 * 60))).thenReturn(3);

        assertThatThrownBy(() -> useCase.execute(userId, PlacementPool.BAND_3_5))
                .isInstanceOfSatisfying(LearningRequestException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(429);
                    assertThat(error.getCode()).isEqualTo("PLACEMENT_LIMIT_REACHED");
                });
        verifyNoInteractions(content, lock);
    }

    @Test
    void assignmentExactlyTwentyFourHoursOldIsOutsideTheLimit() {
        when(assignments.countSince(userId, NOW.minusSeconds(24 * 60 * 60))).thenReturn(0);
        when(content.getPlacementPackages(PlacementPool.BAND_3_5)).thenReturn(List.of(packageIn(PlacementPool.BAND_3_5)));

        assertThat(useCase.execute(userId, PlacementPool.BAND_3_5)).isNotNull();
    }

    @Test
    void anExistingPlacementDoesNotPreventAnotherAssignment() {
        when(placements.find(userId)).thenReturn(Optional.of(com.ieltspath.learning.domain.aggregate.LearnerPlacement
                .create(userId, new com.ieltspath.learning.domain.vo.BandLevel(new java.math.BigDecimal("5.5")),
                        UUID.randomUUID(), NOW)));
        when(content.getPlacementPackages(PlacementPool.BAND_3_5)).thenReturn(List.of(packageIn(PlacementPool.BAND_3_5)));

        assertThat(useCase.execute(userId, PlacementPool.BAND_3_5).pool()).isEqualTo(PlacementPool.BAND_3_5);
    }

    @Test
    void emptyPoolReturnsNotFound() {
        when(content.getPlacementPackages(PlacementPool.BAND_3_5)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(userId, PlacementPool.BAND_3_5))
                .isInstanceOfSatisfying(LearningRequestException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(404);
                    assertThat(error.getCode()).isEqualTo("NO_PLACEMENT_TEST");
                });
    }

    private LearningContentClient.PlacementPackage packageIn(PlacementPool pool) {
        return new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "TEST", pool);
    }
}
