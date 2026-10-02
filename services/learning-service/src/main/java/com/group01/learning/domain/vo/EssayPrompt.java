package com.group01.learning.application.writing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What an essay is graded against, frozen with the submission so a resend never reads Content again.
 * {@code chartFacts} (Task 1) and {@code sampleAnswer} never reach the learner before the rules allow it.
 */
public record EssayPrompt(UUID questionVersionId, String stem, String task, Integer minWords, BigDecimal passBand,
                          String chartFacts, String sampleAnswer, List<Image> images,
                          List<UUID> knowledgePointIds) {
    public record Image(String mediaUrl, String altText) {}
}
