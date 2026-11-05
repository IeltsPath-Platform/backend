package com.ieltspath.learning.domain.vo;

import java.util.Locale;

public enum CourseUnlockTarget {
    READING, LISTENING, WRITING, SPEAKING, COMBO;

    public LearningSkill skill() {
        return this == COMBO ? null : LearningSkill.valueOf(name());
    }

    public static CourseUnlockTarget parse(String value) {
        if (value == null) throw new IllegalArgumentException("Unlock target is required");
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported unlock target", exception);
        }
    }
}
