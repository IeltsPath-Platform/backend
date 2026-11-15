package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.WritingBandCaps.Density;
import com.ieltspath.learning.domain.service.WritingBandCaps.DensityRules;
import com.ieltspath.learning.domain.service.WritingBandCaps.Reason;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingTask;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WritingErrorDensityTest {
    private static final String ESSAY = "The table talking about visits. It have five group. This sentence is fine.\n"
            + "They was go library.";

    private static WritingGrade.Correction correction(String excerpt, String category, String code, String certainty) {
        return new WritingGrade.Correction(excerpt, "x", category, code, code, "", 1, null, null, null, certainty);
    }

    @Test
    void countsTheSentencesThatCarryAnErrorOnceEach() {
        Density density = WritingErrorDensity.measure(ESSAY, List.of(
                correction("talking about", "GRAMMAR", "VERB_FORM", "ERROR"),
                correction("visits", "GRAMMAR", "NOUN_PLURAL", "ERROR"),
                correction("It have", "GRAMMAR", "SUBJECT_VERB_AGREEMENT", "ERROR"),
                correction("They was go", "GRAMMAR", "VERB_FORM", "ERROR")), null);

        assertEquals(4, density.sentences());
        assertEquals(3, density.grammarSentences(), "two errors in the first sentence count as one sentence");
    }

    @Test
    void aSuggestionOfABetterWordingIsNotAnError() {
        Density density = WritingErrorDensity.measure(ESSAY, List.of(
                correction("This sentence is fine", "GRAMMAR", "WORD_CHOICE", "SUGGESTION")), null);

        assertEquals(0, density.grammarSentences());
    }

    @Test
    void aWrongWordFormOrPluralCountsAsAVocabularyErrorToo() {
        Density density = WritingErrorDensity.measure(ESSAY, List.of(
                correction("five group", "GRAMMAR", "NOUN_PLURAL", "ERROR"),
                correction("library", "VOCABULARY", "WORD_CHOICE", "ERROR")), null);

        assertEquals(2, density.vocabularySentences());
        assertEquals(1, density.grammarSentences());
    }

    @Test
    void aWrongLinkingWordOrReferenceCountsAsACoherenceError() {
        Density density = WritingErrorDensity.measure(ESSAY, List.of(
                correction("It have", "COHERENCE", "REFERENCE", "ERROR"),
                correction("They was", "COHERENCE", "LINKER_MISUSE", "ERROR"),
                correction("This sentence", "COHERENCE", "REFERENCE", "SUGGESTION")), null);

        assertEquals(2, density.coherenceSentences());
        assertEquals(0, density.grammarSentences());
    }

    private static WritingGrade.Correction impeding(String excerpt, String category, String certainty, Boolean impedes) {
        return new WritingGrade.Correction(excerpt, "x", category, "CODE", "CODE", "", 1, null, null, null, certainty, impedes);
    }

    @Test
    void onlyAGrammarErrorFlaggedAsImpedingMeaningCountsAsImpeding() {
        Density density = WritingErrorDensity.measure(ESSAY, List.of(
                impeding("It have", "GRAMMAR", "ERROR", true),
                impeding("five group", "GRAMMAR", "ERROR", true),
                impeding("The table talking", "GRAMMAR", "ERROR", false),
                impeding("They was", "GRAMMAR", "SUGGESTION", true),
                impeding("go library", "VOCABULARY", "ERROR", true),
                impeding("This sentence", "GRAMMAR", "ERROR", null)), null);

        assertEquals(1, density.impedingGrammarSentences(), "two flagged errors in one sentence count once");
        assertEquals(3, density.grammarSentences());
    }

    @Test
    void task1CountsTheChartStatementsAndAWrongOverview() {
        var checks = new WritingGrade.Task1Checks(List.of(
                new WritingGrade.DataCheck("a", WritingGrade.DataCheck.WRONG, "x"),
                new WritingGrade.DataCheck("b", WritingGrade.DataCheck.WRONG, "x"),
                new WritingGrade.DataCheck("c", WritingGrade.DataCheck.CORRECT, null),
                new WritingGrade.DataCheck("d", WritingGrade.DataCheck.UNVERIFIABLE, null)), 1, true, false, false, null);

        Density density = WritingErrorDensity.measure(ESSAY, List.of(), checks);

        assertEquals(3, density.dataStatements(), "unverifiable statements are not counted");
        assertEquals(2, density.wrongDataStatements());
        assertTrue(density.overviewWrong());
    }

    private static final DensityRules RULES = new DensityRules(new BigDecimal("0.8"), new BigDecimal("0.5"),
            new BigDecimal("0.3"), new BigDecimal("0.5"), new BigDecimal("0.5"), new BigDecimal("4.0"),
            new BigDecimal("4.0"), new BigDecimal("4.5"), new BigDecimal("4.0"), new BigDecimal("4.0"),
            new BigDecimal("3.0"));

    @Test
    void grammarErrorsInFourSentencesOfFiveCapGra() {
        var limits = WritingBandCaps.evaluateDensity(WritingTask.TASK_2, new Density(5, 4, 0, 0, 0, 0, 0, false), RULES);

        assertEquals(1, limits.size());
        assertEquals(Reason.DENSE_GRAMMAR, limits.getFirst().reason());
        assertEquals("GRA", limits.getFirst().criterion());
        assertEquals(new BigDecimal("4.0"), limits.getFirst().cap());
    }

    @Test
    void justUnderTheShareAddsNothing() {
        assertTrue(WritingBandCaps.evaluateDensity(WritingTask.TASK_2, new Density(10, 7, 4, 0, 0, 0, 0, false), RULES).isEmpty());
    }

    @Test
    void coherenceErrorsInThreeSentencesOfTenCapCc() {
        var limits = WritingBandCaps.evaluateDensity(WritingTask.TASK_1, new Density(10, 0, 0, 3, 0, 0, 0, false), RULES);

        assertEquals(1, limits.size());
        assertEquals(Reason.DENSE_COHERENCE, limits.getFirst().reason());
        assertEquals("CC", limits.getFirst().criterion());
        assertEquals(new BigDecimal("4.5"), limits.getFirst().cap());
    }

    @Test
    void impedingGrammarInFiveSentencesOfTenCapsGra() {
        var limits = WritingBandCaps.evaluateDensity(WritingTask.TASK_2, new Density(10, 5, 0, 0, 5, 0, 0, false), RULES);

        assertEquals(1, limits.size());
        assertEquals(Reason.DENSE_IMPEDING_GRAMMAR, limits.getFirst().reason());
        assertEquals("GRA", limits.getFirst().criterion());
        assertEquals(new BigDecimal("4.0"), limits.getFirst().cap());
    }

    @Test
    void impedingGrammarInFourSentencesOfTenAddsNothing() {
        assertTrue(WritingBandCaps.evaluateDensity(WritingTask.TASK_2, new Density(10, 6, 0, 0, 4, 0, 0, false), RULES).isEmpty());
    }

    @Test
    void coherenceErrorsInTwoSentencesOfTenAddNothing() {
        assertTrue(WritingBandCaps.evaluateDensity(WritingTask.TASK_2, new Density(10, 0, 0, 2, 0, 0, 0, false), RULES).isEmpty());
    }

    @Test
    void wrongChartStatementsCapTaAndAWrongOverviewCapsItLower() {
        var limits = WritingBandCaps.evaluateDensity(WritingTask.TASK_1, new Density(8, 0, 0, 0, 0, 8, 5, true), RULES);

        assertEquals(List.of(Reason.WRONG_DATA, Reason.WRONG_DATA_OVERVIEW), limits.stream().map(WritingBandCaps.Limit::reason).toList());
        assertEquals(new BigDecimal("3.0"), limits.get(1).cap());
    }

    @Test
    void theChartRulesDoNotApplyToTaskTwo() {
        assertTrue(WritingBandCaps.evaluateDensity(WritingTask.TASK_2, new Density(8, 0, 0, 0, 0, 8, 8, true), RULES).isEmpty());
    }

    @Test
    void aRuleWithoutACapIsOff() {
        assertTrue(WritingBandCaps.evaluateDensity(WritingTask.TASK_1, new Density(5, 5, 5, 0, 0, 5, 5, true), DensityRules.NONE).isEmpty());
    }
}
