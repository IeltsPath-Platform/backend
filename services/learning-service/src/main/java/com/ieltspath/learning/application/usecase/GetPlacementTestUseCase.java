package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.PlacementTestResult;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Gives a learner without a placement the published placement test; a learner can sit it only once. */
@Service
public class GetPlacementTestUseCase {
    private final LearnerPlacementRepository placements;
    private final LearningContentClient content;

    public GetPlacementTestUseCase(LearnerPlacementRepository placements, LearningContentClient content) {
        this.placements = placements;
        this.content = content;
    }

    public PlacementTestResult execute(UUID userId) {
        if (placements.find(userId).isPresent()) {
            throw new LearningRequestException(409, "PLACEMENT_ALREADY_DONE", "The placement test was already taken");
        }
        var packages = content.getPlacementTestPackages();
        if (packages == null || packages.isEmpty()) {
            throw new LearningRequestException(404, "NO_PLACEMENT_TEST", "No placement test is published");
        }
        var chosen = packages.getFirst();
        return new PlacementTestResult(chosen.packageId(), chosen.packageVersionId());
    }
}
