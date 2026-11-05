package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What an essay is graded against, frozen with the submission so a resend never reads Content again.
 * {@code chartFacts} (Task 1) and {@code sampleAnswer} never reach the learner before the rules allow it.
 * {@code images} is empty for a prompt stored before images were kept with it, or for a task without a chart.
 */
public record EssayPrompt(UUID questionVersionId, String stem, String task, Integer minWords, BigDecimal passBand,
                          String chartFacts, String sampleAnswer, List<Image> images,
                          List<UUID> knowledgePointIds) {
    public EssayPrompt {
        images = images == null ? List.of() : images;
    }

    public record Image(String mediaUrl, String altText) {}
}
