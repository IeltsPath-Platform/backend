package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.PlacementPicker;
import com.ieltspath.learning.application.result.PlacementTestResult;
import com.ieltspath.learning.application.service.PlacementCatalog;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Deprecated. The placement exam for clients that predate the pools (the mobile app): a learner without a placement
 * gets an exam of the full-length pool, exactly as before, and a learner with one gets {@code 409
 * PLACEMENT_ALREADY_DONE}. It records no assignment; Assessment still caps the band at the pool of the exam and applies
 * its own limit on started attempts. The web app uses the pool routes in {@link AssignPlacementTestUseCase}.
 */
@Service
public class GetLegacyPlacementTestUseCase {
    private final LearnerPlacementRepository placements;
    private final PlacementCatalog catalog;
    private final PlacementPicker picker;

    public GetLegacyPlacementTestUseCase(LearnerPlacementRepository placements, PlacementCatalog catalog,
                                         PlacementPicker picker) {
        this.placements = placements;
        this.catalog = catalog;
        this.picker = picker;
    }

    public PlacementTestResult execute(UUID userId) {
        if (placements.find(userId).isPresent()) {
            throw new LearningRequestException(409, "PLACEMENT_ALREADY_DONE", "The placement test was already taken");
        }
        var packages = catalog.packages(PlacementPool.BAND_5_9);
        if (packages.isEmpty()) {
            throw new LearningRequestException(404, "NO_PLACEMENT_TEST", "No placement test is published");
        }
        var chosen = packages.get(picker.choose(packages.size()));
        return new PlacementTestResult(chosen.packageId(), chosen.packageVersionId());
    }
}
