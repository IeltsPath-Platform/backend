package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.PlacementCatalog;
import com.ieltspath.learning.domain.aggregate.LearnerPlacement;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.vo.BandLevel;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The first-time exam for clients that predate the pools: the full-length pool only, and only before a placement. */
class GetLegacyPlacementTestUseCaseTest {
    private final LearnerPlacementRepository placements = mock(LearnerPlacementRepository.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final PlacementCatalog catalog = new PlacementCatalog(content, Clock.systemUTC(), 30);
    private final UUID user = UUID.randomUUID();

    @Test
    void handsOutAnExamOfTheFullLengthPoolAndNeverAShortenedOne() {
        var shortened = new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "LOW", PlacementPool.BAND_0_3);
        var middle = new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "MID", PlacementPool.BAND_3_5);
        var first = new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "HIGH-1", PlacementPool.BAND_5_9);
        var second = new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "HIGH-2", PlacementPool.BAND_5_9);
        when(content.getPlacementPackages()).thenReturn(List.of(shortened, middle, first, second));
        when(placements.find(user)).thenReturn(Optional.empty());

        var picksSecond = new GetLegacyPlacementTestUseCase(placements, catalog, bound -> bound - 1).execute(user);
        var picksFirst = new GetLegacyPlacementTestUseCase(placements, catalog, bound -> 0).execute(user);

        assertThat(picksSecond.packageVersionId()).isEqualTo(second.packageVersionId());
        assertThat(picksFirst.packageVersionId()).isEqualTo(first.packageVersionId());
    }

    @Test
    void aLearnerWhoAlreadyHasAPlacementGetsTheOldConflict() {
        when(placements.find(user)).thenReturn(Optional.of(LearnerPlacement.create(user,
                new BandLevel(new BigDecimal("5.5")), UUID.randomUUID(), Instant.now())));

        assertThatThrownBy(() -> new GetLegacyPlacementTestUseCase(placements, catalog, bound -> 0).execute(user))
                .isInstanceOfSatisfying(LearningRequestException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(409);
                    assertThat(error.getCode()).isEqualTo("PLACEMENT_ALREADY_DONE");
                });
    }

    @Test
    void withoutAPublishedFullLengthExamTheAnswerIsNotFound() {
        when(placements.find(user)).thenReturn(Optional.empty());
        when(content.getPlacementPackages()).thenReturn(List.of(
                new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "LOW", PlacementPool.BAND_0_3)));

        assertThatThrownBy(() -> new GetLegacyPlacementTestUseCase(placements, catalog, bound -> 0).execute(user))
                .isInstanceOfSatisfying(LearningRequestException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(404);
                    assertThat(error.getCode()).isEqualTo("NO_PLACEMENT_TEST");
                });
    }
}
