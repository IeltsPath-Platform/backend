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

    @Test
    void rendersTopicCourseAndSkillTokensWithPaddedSequence() {
        patterns.validate("TOPIC", "{COURSE}_{SKILL}_T{SEQ:2}");
        assertEquals("COURSE_001_R_T03", patterns.render("TOPIC", "{COURSE}_{SKILL}_T{SEQ:2}", 3,
                Map.of("course", "COURSE_001", "skill", "R")));
    }

    @Test
    void rejectsTopicTokensThatDoNotApplyAndMissingUniquePart() {
        assertThrows(IllegalArgumentException.class, () -> patterns.validate("TOPIC", "{TYPE}_{SEQ}"));
        assertThrows(IllegalArgumentException.class, () -> patterns.validate("TOPIC", "{COURSE}_{SKILL}"));
    }

    @Test
    void rendersKnowledgePointPrefixLevelAndNormalizedName() {
        patterns.validate("KNOWLEDGE_POINT", "{PREFIX}{LEVEL}_{NAME}");
        assertEquals("R55_READ_SHORT_TEXTS", patterns.render("KNOWLEDGE_POINT", "{PREFIX}{LEVEL}_{NAME}", 1,
                Map.of("prefix", "R", "level", "55", "name", "Read short texts")));
        assertEquals("G70_COMPLEX_SENTENCES", patterns.render("KNOWLEDGE_POINT", "{PREFIX}{LEVEL}_{NAME}", 1,
                Map.of("prefix", "G", "level", "70", "name", "Complex sentences")));
        assertThrows(IllegalArgumentException.class,
                () -> patterns.validate("KNOWLEDGE_POINT", "{SKILL}_{NAME}"));
    }

    @Test
    void everyPackageTypeHasAStableCodeAbbreviation() {
        var abbreviations = java.util.Arrays.stream(com.ieltspath.content.domain.vo.PackageType.values())
                .map(com.ieltspath.content.domain.vo.PackageType::codeAbbreviation).toList();
        assertEquals(java.util.List.of("MT", "PL", "PS", "PT", "QZ", "LS", "TT", "CT"), abbreviations);
    }
}
