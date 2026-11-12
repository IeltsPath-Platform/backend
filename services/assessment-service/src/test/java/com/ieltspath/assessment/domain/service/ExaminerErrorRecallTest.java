package com.ieltspath.assessment.domain.service;

import com.ieltspath.assessment.domain.service.ExaminerErrorRecall.Counts;
import com.ieltspath.assessment.domain.service.ExaminerErrorRecall.Kind;
import com.ieltspath.assessment.domain.service.ExaminerErrorRecall.MarkedError;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExaminerErrorRecallTest {
    private static final String ESSAY = "The number was going down, and peoples can save more money.";

    @Test
    void anExactMatchIsCaptured() {
        Counts counts = ExaminerErrorRecall.score(ESSAY,
                List.of(new MarkedError("peoples can save", Kind.LANGUAGE)), List.of("peoples can save"));

        assertEquals(new BigDecimal("1.00000"), counts.languageRecall());
    }

    @Test
    void aPartialOverlapIsCaptured() {
        Counts counts = ExaminerErrorRecall.score(ESSAY,
                List.of(new MarkedError("peoples can save", Kind.LANGUAGE)), List.of("can save more"));

        assertEquals(1, counts.languageCaptured());
    }

    @Test
    void aCorrectionOnOtherWordsIsNotCaptured() {
        Counts counts = ExaminerErrorRecall.score(ESSAY,
                List.of(new MarkedError("peoples can save", Kind.LANGUAGE)), List.of("money"));

        assertEquals(0, counts.languageCaptured());
        assertEquals(new BigDecimal("0.00000"), counts.languageRecall());
    }

    @Test
    void aRepeatedPhraseCountsWhicheverOccurrenceTheModelPointedAt() {
        String essay = "bad idea. bad idea again.";
        Counts counts = ExaminerErrorRecall.score(essay,
                List.of(new MarkedError("bad idea", Kind.LANGUAGE)), List.of("bad idea again"));

        assertEquals(1, counts.languageCaptured());
        assertEquals(1, counts.languageTotal());
    }

    @Test
    void dataErrorsAreScoredApartFromLanguageErrors() {
        Counts counts = ExaminerErrorRecall.score(ESSAY, List.of(
                        new MarkedError("was going down", Kind.DATA),
                        new MarkedError("peoples can save", Kind.LANGUAGE)),
                List.of("was going down"));

        assertEquals(new BigDecimal("1.00000"), counts.dataRecall());
        assertEquals(new BigDecimal("0.00000"), counts.languageRecall());
    }

    @Test
    void anEssayWithoutMarkedErrorsHasNoRecallRate() {
        Counts counts = ExaminerErrorRecall.score(ESSAY, List.of(), List.of("money"));

        assertEquals(Counts.NONE, counts);
        assertNull(counts.languageRecall());
        assertNull(counts.dataRecall());
    }

    @Test
    void anExcerptNotFoundInTheEssayIsNotCounted() {
        Counts counts = ExaminerErrorRecall.score(ESSAY,
                List.of(new MarkedError("not in the essay", Kind.LANGUAGE)), List.of("not in the essay"));

        assertEquals(0, counts.languageTotal());
        assertNull(counts.languageRecall());
    }

    @Test
    void countsAddUpAcrossEssays() {
        Counts one = new Counts(2, 1, 0, 0);
        Counts two = new Counts(2, 2, 1, 0);

        assertEquals(new BigDecimal("0.75000"), one.plus(two).languageRecall());
        assertEquals(new BigDecimal("0.00000"), one.plus(two).dataRecall());
    }
}
