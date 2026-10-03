package com.group01.learning.domain.vo;

import java.util.List;
import java.util.UUID;

/** A catalog knowledge point with the learner's last five outcomes (oldest first) and total evidence count. */
public record MasteryHistory(UUID knowledgePointId, UUID topicId, boolean hasPracticeSet, List<Boolean> correctness,
                             long evidenceCount, LearningSkill skill) {
    public MasteryHistory(UUID knowledgePointId, UUID topicId, boolean hasPracticeSet, List<Boolean> correctness,
                          long evidenceCount) {
        this(knowledgePointId, topicId, hasPracticeSet, correctness, evidenceCount, null);
    }
    public MasteryHistory {
        correctness = List.copyOf(correctness);
    }
}
