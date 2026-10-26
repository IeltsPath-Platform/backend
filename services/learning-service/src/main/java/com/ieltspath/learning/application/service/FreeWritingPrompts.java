package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** The writing prompts a learner may write free essays for: the essay questions of a practice test. */
@Component
public class FreeWritingPrompts {
    private static final String PRACTICE_TEST = "PRACTICE_TEST";

    private final LearningContentClient content;

    public FreeWritingPrompts(LearningContentClient content) {
        this.content = content;
    }

    /** The essay question of the practice test; 422 when the package is not a practice test, 404 when it has no such essay. */
    public Item find(UUID packageVersionId, UUID questionVersionId) {
        var version = content.getPackageVersion(packageVersionId);
        if (!PRACTICE_TEST.equals(version.packageType())) {
            throw new LearningRequestException(422, "NOT_A_PRACTICE_TEST",
                    "Free writing is only available for the prompts of a practice test");
        }
        return ItemGrading.orderedItems(version).stream()
                .filter(candidate -> candidate.questionVersionId().equals(questionVersionId))
                .filter(ItemGrading::isEssay).findFirst()
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND",
                        "This writing prompt was not found in the test"));
    }
}
