package com.group01.learning.domain.vo;

import java.util.List;
import java.util.UUID;

/** A catalog knowledge point with the learner's last five outcomes (oldest first) and total evidence count. */
public record MasteryHistory(UUID knowledgePointId, UUID topicId, boolean hasPracticeSet, List<Boolean> correctness,
                             long evidenceCount) {
    public MasteryHistory {
        correctness = List.copyOf(correctness);
    }
}
