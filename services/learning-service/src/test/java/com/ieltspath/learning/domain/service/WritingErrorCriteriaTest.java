package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WritingErrorCriteriaTest {
    private static final UUID WORDS = UUID.randomUUID();
    private static final UUID PARAPHRASE = UUID.randomUUID();
    private static final UUID ARTICLES = UUID.randomUUID();
    private static final UUID LINKING = UUID.randomUUID();

    private static WritingGrade.Correction error(String category, String code, int severity, UUID point) {
        return new WritingGrade.Correction("x", "y", category, code, code, "", severity, point, null, null,
                WritingGrade.Correction.ERROR);
    }

    @Test
    void theCodeMetMostOftenComesFirst() {
        var corrections = List.of(error("VOCABULARY", "REPETITION", 3, PARAPHRASE),
                error("VOCABULARY", "WORD_CHOICE", 1, WORDS), error("VOCABULARY", "WORD_CHOICE", 1, WORDS));

        assertEquals(List.of(WORDS, PARAPHRASE), WritingErrorCriteria.reviewPoints(corrections, "LR"));
    }

    @Test
    void aTieGoesToTheMoreSevereCodeThenToTheFirstCode() {
        var bySeverity = List.of(error("VOCABULARY", "WORD_CHOICE", 1, WORDS), error("VOCABULARY", "REPETITION", 2, PARAPHRASE));
        var byName = List.of(error("VOCABULARY", "WORD_CHOICE", 2, WORDS), error("VOCABULARY", "REPETITION", 2, PARAPHRASE));

        assertEquals(List.of(PARAPHRASE, WORDS), WritingErrorCriteria.reviewPoints(bySeverity, "LR"));
        assertEquals(List.of(PARAPHRASE, WORDS), WritingErrorCriteria.reviewPoints(byName, "LR"),
                "REPETITION comes before WORD_CHOICE");
    }

    @Test
    void wordLevelGrammarCodesBelongToLexicalResourceNotGrammar() {
        var corrections = List.of(error("GRAMMAR", "NOUN_PLURAL", 1, ARTICLES), error("GRAMMAR", "ARTICLE", 1, ARTICLES),
                error("GRAMMAR", "WORD_FORM", 1, WORDS));

        assertEquals(List.of(ARTICLES, WORDS), WritingErrorCriteria.reviewPoints(corrections, "LR"));
        assertEquals(List.of(ARTICLES), WritingErrorCriteria.reviewPoints(corrections, "GRA"));
    }

    @Test
    void aPointSharedByTwoCodesIsListedOnce() {
        var corrections = List.of(error("GRAMMAR", "ARTICLE", 1, ARTICLES), error("GRAMMAR", "ARTICLE", 1, ARTICLES),
                error("GRAMMAR", "PLURAL_FORM", 1, ARTICLES));

        assertEquals(List.of(ARTICLES), WritingErrorCriteria.reviewPoints(corrections, "GRA"));
    }

    @Test
    void eachCriterionTakesOnlyItsOwnGroup() {
        var corrections = List.of(error("COHERENCE", "LINKER_MISUSE", 1, LINKING), error("TASK", "T1_NO_OVERVIEW", 1, UUID.randomUUID()),
                error("VOCABULARY", "WORD_CHOICE", 1, WORDS));

        assertEquals(List.of(LINKING), WritingErrorCriteria.reviewPoints(corrections, "CC"));
        assertEquals(1, WritingErrorCriteria.reviewPoints(corrections, "TA").size());
        assertEquals(1, WritingErrorCriteria.reviewPoints(corrections, "TR").size());
        assertEquals(List.of(WORDS), WritingErrorCriteria.reviewPoints(corrections, "LR"));
    }

    @Test
    void suggestionsAndCodesWithoutAPracticePointAreNotCounted() {
        var suggestion = new WritingGrade.Correction("x", "y", "VOCABULARY", "WORD_CHOICE", "n", "", 1, WORDS, null, null,
                WritingGrade.Correction.SUGGESTION);
        var corrections = List.of(suggestion, error("VOCABULARY", "VOCABULARY_OTHER", 1, null));

        assertTrue(WritingErrorCriteria.reviewPoints(corrections, "LR").isEmpty());
    }

    @Test
    void noCorrectionsGivesNoPoints() {
        assertTrue(WritingErrorCriteria.reviewPoints(null, "LR").isEmpty());
        assertTrue(WritingErrorCriteria.reviewPoints(List.of(), "LR").isEmpty());
    }
}
