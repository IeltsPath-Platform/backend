package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.PlacementEssayGrade;
import com.ieltspath.learning.application.result.PlacementGradesResult;
import com.ieltspath.learning.application.result.WritingSubmissionResult;

import java.util.List;
import java.util.UUID;

/**
 * Placement detail in the shapes practice already uses: Speaking as a practice result, each essay as a Writing
 * submission whose id is the attempt item.
 */
public record PlacementGradesResponse(PlacementGradesResult.Speaking speaking, List<Essay> writing) {
    public record Essay(UUID attemptItemId, WritingSubmissionResponse submission) { }

    public static PlacementGradesResponse from(PlacementGradesResult result) {
        return new PlacementGradesResponse(result.speaking(), result.writing().stream()
                .map(essay -> new Essay(essay.attemptItemId(), WritingSubmissionResponse.from(submission(essay))))
                .toList());
    }

    private static WritingSubmissionResult submission(PlacementEssayGrade essay) {
        return new WritingSubmissionResult(essay.attemptItemId(), "GRADED", essay.task(), essay.wordCount(),
                essay.grade().overallBand(), essay.passed(), essay.grade(), null, null, null, null,
                essay.essayText(), null);
    }
}
