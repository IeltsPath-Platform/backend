package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.aggregate.LearnerPlacement;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.vo.BandLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlacementGateUseCasesTest {
    private final LearnerPlacementRepository placements = mock(LearnerPlacementRepository.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final UUID userId = UUID.randomUUID();

    @Test
    void learnerWithoutPlacementReceivesThePublishedPlacementTest() {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(placements.find(userId)).thenReturn(Optional.empty());
        when(content.getPlacementTestPackages()).thenReturn(
                List.of(new LearningContentClient.TestPackage(packageId, versionId, "PLACEMENT")));

        var result = new GetPlacementTestUseCase(placements, content).execute(userId);

        assertThat(result.packageId()).isEqualTo(packageId);
        assertThat(result.packageVersionId()).isEqualTo(versionId);
    }

    @Test
    void placementCanBeTakenOnlyOnce() {
        when(placements.find(userId)).thenReturn(Optional.of(existingPlacement()));

        assertThatThrownBy(() -> new GetPlacementTestUseCase(placements, content).execute(userId))
                .isInstanceOfSatisfying(LearningRequestException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(409);
                    assertThat(error.getCode()).isEqualTo("PLACEMENT_ALREADY_DONE");
                });
    }

    @Test
    void missingPlacementPackageIsReportedAsNotFound() {
        when(placements.find(userId)).thenReturn(Optional.empty());
        when(content.getPlacementTestPackages()).thenReturn(List.of());

        assertThatThrownBy(() -> new GetPlacementTestUseCase(placements, content).execute(userId))
                .isInstanceOfSatisfying(LearningRequestException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(404);
                    assertThat(error.getCode()).isEqualTo("NO_PLACEMENT_TEST");
                });
    }

    @Test
    void learnerWithoutPlacementIsRefused() {
        when(placements.find(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RequirePlacementUseCase(placements).execute(userId))
                .isInstanceOfSatisfying(LearningRequestException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(403);
                    assertThat(error.getCode()).isEqualTo("PLACEMENT_REQUIRED");
                });
    }

    @Test
    void learnerWithPlacementPasses() {
        when(placements.find(userId)).thenReturn(Optional.of(existingPlacement()));

        new RequirePlacementUseCase(placements).execute(userId);
    }

    private LearnerPlacement existingPlacement() {
        return LearnerPlacement.create(userId, new BandLevel(new BigDecimal("5.5")), UUID.randomUUID(), Instant.now());
    }
}
