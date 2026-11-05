package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshLearningTopicsSkillTest {

    @Test
    void aPointOfOneSkillKeepsIt() {
        assertEquals(LearningSkill.READING, RefreshLearningTopicsUseCase.skillOf("READING", LearningSkill.WRITING));
    }

    @Test
    void aPointForAllSkillsTakesTheSkillOfItsTopic() {
        assertEquals(LearningSkill.WRITING, RefreshLearningTopicsUseCase.skillOf("ALL", LearningSkill.WRITING));
    }

    @Test
    void aPointWithoutASkillTakesTheSkillOfItsTopic() {
        assertEquals(LearningSkill.LISTENING, RefreshLearningTopicsUseCase.skillOf(null, LearningSkill.LISTENING));
        assertEquals(LearningSkill.LISTENING, RefreshLearningTopicsUseCase.skillOf(" ", LearningSkill.LISTENING));
    }

    @Test
    void aSkillNobodyKnowsIsStillRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RefreshLearningTopicsUseCase.skillOf("COOKING", LearningSkill.READING));
    }
}
