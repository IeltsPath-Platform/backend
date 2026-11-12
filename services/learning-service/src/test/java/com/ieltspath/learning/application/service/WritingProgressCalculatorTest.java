package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.result.WritingProgressResult.TaskProgress;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingProgressEssay;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WritingProgressCalculatorTest {
    private static final Instant BASE = Instant.parse("2026-11-01T00:00:00Z");

    private final WritingProgressCalculator calculator = new WritingProgressCalculator(2);

    @Test
    void anErrorCountsOncePerEssayEvenWhenListedTwice() {
        List<WritingProgressEssay> essays = List.of(
                essay(0, correction("TENSE"), correction("TENSE")),
                essay(1), essay(2));

        TaskProgress progress = calculate(essays);

        assertEquals(1, progress.recurringErrors().getFirst().essaysWithError());
        assertEquals(3, progress.recurringErrors().getFirst().essaysTotal());
    }

    @Test
    void recurringErrorsSortByEssaysThenSeverityAndCatchAllsLast() {
        List<WritingProgressEssay> essays = List.of(
                essay(0, correction("ARTICLE", 1), correction("TENSE", 3), correction("MISC_OTHER", 3)),
                essay(1, correction("TENSE", 3)),
                essay(2, correction("ARTICLE", 1), correction("TENSE", 3)));

        List<String> codes = calculate(essays).recurringErrors().stream()
                .map(error -> error.errorCode()).toList();

        assertEquals(List.of("TENSE", "ARTICLE", "MISC_OTHER"), codes);
    }

    @Test
    void anErrorIsFixedOnlyAfterEnoughCleanEssaysSinceItsLastOccurrence() {
        List<WritingProgressEssay> twoCleanAfter = List.of(
                essay(0, correction("TENSE")), essay(1), essay(2));
        List<WritingProgressEssay> oneCleanAfter = List.of(
                essay(0, correction("TENSE")), essay(1, correction("TENSE")), essay(2));

        TaskProgress fixed = calculate(twoCleanAfter);
        assertEquals(1, fixed.fixedErrors().size());
        assertEquals("TENSE", fixed.fixedErrors().getFirst().errorCode());
        assertEquals(2, fixed.fixedErrors().getFirst().cleanEssaysSince());

        assertTrue(calculate(oneCleanAfter).fixedErrors().isEmpty());
    }

    @Test
    void anErrorStillInTheLatestEssaysIsNotFixed() {
        List<WritingProgressEssay> essays = List.of(
                essay(0), essay(1, correction("TENSE")), essay(2, correction("TENSE")));

        assertTrue(calculate(essays).fixedErrors().isEmpty());
    }

    @Test
    void catchAllCodesAreNeverFixed() {
        List<WritingProgressEssay> essays = List.of(
                essay(0, correction("MISC_OTHER")), essay(1), essay(2), essay(3));

        assertTrue(calculate(essays).fixedErrors().isEmpty());
    }

    @Test
    void fewerThanThreeEssaysAreMarkedInsufficient() {
        assertTrue(calculate(List.of(essay(0), essay(1))).insufficient());
        assertFalse(calculate(List.of(essay(0), essay(1), essay(2))).insufficient());
    }

    @Test
    void storedNamesWinAndMissingNamesFallBackToContentTexts() {
        WritingProgressEssay stored = essay(0, correction("TENSE", "Tense", "Use the past tense"));
        WritingProgressEssay legacy = essay(1, correction("ARTICLE"));
        Map<String, ErrorTypeTexts.Text> texts = Map.of(
                "ARTICLE", new ErrorTypeTexts.Text("Article", "Add a/an before a singular noun"),
                "TENSE", new ErrorTypeTexts.Text("Other name", "Other hint"));

        TaskProgress progress = calculator.calculate("TASK_2", List.of(legacy, stored), texts);

        Map<String, String> names = progress.recurringErrors().stream().collect(
                java.util.stream.Collectors.toMap(error -> error.errorCode(), error -> error.errorName()));
        assertEquals("Tense", names.get("TENSE"));
        assertEquals("Article", names.get("ARTICLE"));
        assertEquals("Add a/an before a singular noun",
                progress.recurringErrors().stream().filter(error -> error.errorCode().equals("ARTICLE"))
                        .findFirst().orElseThrow().fixHint());
    }

    @Test
    void missingTextsLeaveTheNameEmptyWithoutFailing() {
        TaskProgress progress = calculator.calculate("TASK_1",
                List.of(essay(1, correction("ARTICLE")), essay(0, correction("ARTICLE"))), null);

        assertNull(progress.recurringErrors().getFirst().errorName());
    }

    @Test
    void pointsRunOldestFirstWithOnlyTheCriteriaBands() {
        List<WritingProgressEssay> newestFirst = List.of(essay(2), essay(1), essay(0));

        List<String> sources = calculator.calculate("TASK_2", newestFirst, Map.of()).points().stream()
                .map(point -> point.source()).toList();
        Map<String, BigDecimal> criteria = calculator.calculate("TASK_2", newestFirst, Map.of()).points().getFirst()
                .criteria();

        assertEquals(List.of("LESSON", "LESSON", "LESSON"), sources);
        assertEquals(new BigDecimal("6.0"), criteria.get("TR"));
        assertEquals(4, criteria.size());
    }

    @Test
    void recurringErrorsAreCappedAtTen() {
        List<WritingGrade.Correction> many = new ArrayList<>();
        for (int i = 0; i < 12; i++) many.add(correction("CODE_" + i));
        List<WritingProgressEssay> essays = List.of(essay(0, many.toArray(WritingGrade.Correction[]::new)));

        assertEquals(10, calculate(essays).recurringErrors().size());
    }

    private TaskProgress calculate(List<WritingProgressEssay> chronological) {
        List<WritingProgressEssay> newestFirst = new ArrayList<>(chronological);
        Collections.reverse(newestFirst);
        return calculator.calculate("TASK_2", newestFirst, Map.of());
    }

    private static WritingProgressEssay essay(int dayOffset, WritingGrade.Correction... corrections) {
        WritingGrade.Criterion tr = new WritingGrade.Criterion("TR", new BigDecimal("6.0"), List.of(), List.of());
        return new WritingProgressEssay(UUID.randomUUID(), "LESSON_BLOCK", BASE.plusSeconds(dayOffset * 86_400L),
                new BigDecimal("6.0"), "v1", List.of(tr,
                new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of()),
                new WritingGrade.Criterion("LR", new BigDecimal("6.0"), List.of(), List.of()),
                new WritingGrade.Criterion("GRA", new BigDecimal("6.0"), List.of(), List.of())),
                List.of(corrections));
    }

    private static WritingGrade.Correction correction(String code) {
        return new WritingGrade.Correction("excerpt", "fix", "GRAMMAR", code, null, null, null, null);
    }

    private static WritingGrade.Correction correction(String code, int severity) {
        return new WritingGrade.Correction("excerpt", "fix", "GRAMMAR", code, null, null, severity, null);
    }

    private static WritingGrade.Correction correction(String code, String name, String hint) {
        return new WritingGrade.Correction("excerpt", "fix", "GRAMMAR", code, name, hint, 2, null);
    }
}
