package com.ieltspath.library.domain.study;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StudyUnitRulesTest {
    static StudyWordSnapshot word(String lemma, String meaning) {
        return new StudyWordSnapshot(UUID.randomUUID(), UUID.randomUUID(), 1, lemma, "NOUN", meaning,
                null, null, null, null, null, null, "A " + lemma + ".", null, true);
    }
    @Test void requiresFourActiveDistinctMeanings() {
        var words = List.of(word("one", " Caf\u00e9 "), word("two", "CAFE\u0301"), word("three", ""));
        var report = StudyUnitRules.validate(words, List.of());
        assertFalse(report.canPublish());
        assertTrue(report.errors().stream().anyMatch(x -> x.code().equals("TOO_FEW_WORDS")));
        assertTrue(report.errors().stream().anyMatch(x -> x.code().equals("DUPLICATE_MEANING")));
        assertTrue(report.errors().stream().anyMatch(x -> x.code().equals("MISSING_MEANING")));
    }
    @Test void mediaAndMissingFillAreWarningsOnly() {
        var words = List.of(word("one", "a"), word("two", "b"), word("three", "c"), word("four", "d"));
        var report = StudyUnitRules.validate(words, List.of());
        assertTrue(report.canPublish());
        for (String code : List.of("MISSING_IPA", "MISSING_AUDIO", "MISSING_IMAGE", "MISSING_EXAMPLE_TRANSLATION"))
            assertTrue(report.warnings().stream().anyMatch(x -> x.code().equals(code)), code);
    }
    @Test void detectsInactiveRepeatedItemAndWordBoundary() {
        var first = word("art", "nghệ thuật");
        var other = new StudyWordSnapshot(first.vocabularyItemId(), UUID.randomUUID(), 2, "art", "NOUN", "kỹ năng",
                null, null, null, null, null, null, "A party.", null, false);
        var report = StudyUnitRules.validate(List.of(first, other), List.of());
        assertTrue(report.errors().stream().anyMatch(x -> x.code().equals("INACTIVE_SENSE")));
        assertTrue(report.warnings().stream().anyMatch(x -> x.code().equals("SAME_WORD_TWICE")));
        assertTrue(report.warnings().stream().anyMatch(x -> x.code().equals("EXAMPLE_WITHOUT_LEMMA")));
    }
}
