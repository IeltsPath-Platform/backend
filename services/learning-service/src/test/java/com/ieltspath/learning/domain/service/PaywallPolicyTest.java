package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaywallPolicyTest {
    private static final UUID COURSE = UUID.randomUUID();

    @Test
    void courseLessAndFirstThreeLessonsAreOpenWithoutPremium() {
        assertTrue(PaywallPolicy.isOpen(null, 20, Set.of(LearningSkill.READING), Set.of(), false,
                false, false));
        for (int position = 1; position <= 3; position++) {
            assertTrue(PaywallPolicy.isOpen(COURSE, position, Set.of(LearningSkill.READING), Set.of(), false,
                    false, false));
        }
        assertFalse(PaywallPolicy.isOpen(COURSE, 4, Set.of(LearningSkill.READING), Set.of(), false,
                false, false));
    }

    @Test
    void activeSkillComboGrandfatheringAndPremiumOpenTheLesson() {
        assertTrue(open(Set.of(LearningSkill.READING), false, false, false));
        assertTrue(open(Set.of(), true, false, false));
        assertTrue(open(Set.of(), false, true, false));
        assertTrue(open(Set.of(), false, false, true));
    }

    @Test
    void owningAnotherSkillDoesNotOpenTheLesson() {
        assertFalse(open(Set.of(LearningSkill.LISTENING), false, false, false));
    }

    private static boolean open(Set<LearningSkill> owned, boolean combo, boolean grandfathered,
                                boolean premium) {
        return PaywallPolicy.isOpen(COURSE, 4, Set.of(LearningSkill.READING), owned, combo, grandfathered,
                premium);
    }
}
