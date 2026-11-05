package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.Set;
import java.util.UUID;

/** Pure rule for opening a lesson or knowledge point behind the course paywall. */
public final class PaywallPolicy {
    public static final int FREE_LESSONS_PER_SKILL = 3;

    private PaywallPolicy() {}

    public static boolean isOpen(UUID courseId, Integer skillPosition, Set<LearningSkill> lessonSkills,
                                  Set<LearningSkill> unlockedSkills, boolean comboUnlocked,
                                  boolean grandfathered, boolean premium) {
        if (courseId == null) return true;
        if (skillPosition != null && skillPosition <= FREE_LESSONS_PER_SKILL) return true;
        if (comboUnlocked || intersects(lessonSkills, unlockedSkills) || grandfathered || premium) return true;
        return false;
    }

    private static boolean intersects(Set<LearningSkill> lessonSkills, Set<LearningSkill> unlockedSkills) {
        return lessonSkills != null && unlockedSkills != null
                && lessonSkills.stream().anyMatch(unlockedSkills::contains);
    }
}
