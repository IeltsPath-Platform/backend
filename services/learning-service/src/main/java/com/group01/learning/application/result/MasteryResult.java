package com.group01.learning.application.result;

import java.util.UUID;
import com.group01.learning.domain.vo.LearningSkill;

public record MasteryResult(UUID knowledgePointId, UUID topicId, double mastery, long evidenceCount,
                            LearningSkill skill) {
    public MasteryResult(UUID knowledgePointId, UUID topicId, double mastery, long evidenceCount) {
        this(knowledgePointId, topicId, mastery, evidenceCount, null);
    }
}
