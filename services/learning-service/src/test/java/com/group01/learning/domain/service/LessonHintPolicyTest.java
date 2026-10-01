package com.group01.learning.domain.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LessonHintPolicyTest {
    private final LessonHintPolicy policy = new LessonHintPolicy();

    @Test
    void eligibleFillAndChoiceIncludeThreeOptionsAndEmptyTfngOptions() {
        assertTrue(policy.eligible(Map.of("type", "FILL", "accepted", List.of("word")), 0));
        assertTrue(policy.eligible(Map.of("type", "CHOICE", "correct", "A"), 3));
        assertTrue(policy.eligible(Map.of("type", "CHOICE", "correct", "A"), 4));
        assertTrue(policy.eligible(Map.of("type", "CHOICE", "correct", "TRUE"), 0));
        assertTrue(policy.eligible(Map.of("correct", "A"), 3));
        assertFalse(policy.eligible(Map.of("type", "CHOICE", "correct", "A"), 2));
        assertFalse(policy.eligible(Map.of("type", "CHOICE", "correct", "A"), 1));
    }

    @Test
    void incompleteAndUngradableSpecsNeverExposeHints() {
        assertFalse(policy.eligible(null, 3));
        assertFalse(policy.eligible(Map.of(), 3));
        assertFalse(policy.eligible(Map.of("type", "CHOICE"), 3));
        assertFalse(policy.eligible(Map.of("type", "CHOICE", "correct", " "), 3));
        assertFalse(policy.eligible(Map.of("type", "FILL"), 0));
        assertFalse(policy.eligible(Map.of("type", "FILL", "accepted", List.of()), 0));
        assertFalse(policy.eligible(Map.of("type", "FILL", "accepted", List.of("word", " ")), 0));
        assertFalse(policy.eligible(Map.of("type", "ESSAY", "correct", "A"), 3));
    }
}
