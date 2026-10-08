package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** A learner must have a placement band before any course, topic or lesson is available. */
@Service
public class RequirePlacementUseCase {
    private final LearnerPlacementRepository placements;

    public RequirePlacementUseCase(LearnerPlacementRepository placements) {
        this.placements = placements;
    }

    public void execute(UUID userId) {
        if (placements.find(userId).isEmpty()) {
            throw new LearningRequestException(403, "PLACEMENT_REQUIRED", "Take the placement test first");
        }
    }
}
