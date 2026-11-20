package com.ieltspath.content.domain.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CodePatternTest {
    private final CodePattern patterns = new CodePattern();

    @Test
    void rendersKeywordsAndPaddedSequence() {
        assertEquals("COURSE_007_6_0_" + java.time.Year.now().getValue(), patterns.render("COURSE", "COURSE_{SEQ:3}_{BAND}_{YYYY}", 7,
                Map.of("band", "6")));
        assertTrue(patterns.usesSequence("COURSE_{SEQ:3}"));
        assertTrue(patterns.usesSequence("Q-{SEQ}"));
        assertFalse(patterns.usesSequence("IELTS_{BAND}"));
    }

    @Test
    void normalizesNamesByRemovingDiacriticsAndJoiningWords() {
        assertEquals("COLLOCATION", patterns.render("ERROR_TYPE", "{NAME}", 1, Map.of("name", "Collocation")));
        assertEquals("CAFE_AU_LAIT", patterns.render("ERROR_TYPE", "{NAME}", 1, Map.of("name", "Café au lait")));
    }

    @Test
    void rejectsUnknownAndWrongKindKeywords() {
        assertThrows(IllegalArgumentException.class, () -> patterns.validate("COURSE", "C_{MYSTERY}_{SEQ}"));
        assertThrows(IllegalArgumentException.class, () -> patterns.validate("COURSE", "C_{TOPIC}_{SEQ}"));
        assertThrows(IllegalArgumentException.class, () -> patterns.validate("COURSE", "C_{SEQ:abc}"));
    }

    @Test
    void requiresUniquenessComponent() {
        assertThrows(IllegalArgumentException.class, () -> patterns.validate("COURSE", "COURSE_{YYYY}"));
    }
}
