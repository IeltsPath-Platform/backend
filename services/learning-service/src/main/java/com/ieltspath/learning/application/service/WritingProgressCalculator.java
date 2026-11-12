package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.result.WritingProgressResult.FixedError;
import com.ieltspath.learning.application.result.WritingProgressResult.Point;
import com.ieltspath.learning.application.result.WritingProgressResult.RecurringError;
import com.ieltspath.learning.application.result.WritingProgressResult.TaskProgress;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingProgressEssay;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns one task's graded first essays into the progress view. Pure: no clock, no I/O.
 *
 * <p>An error counts once per essay, so a grade that lists more corrections does not look like a rise in errors.
 * Codes ending in {@code _OTHER} are catch-alls: they sort last and never count as fixed.
 */
public final class WritingProgressCalculator {
    /** Below this many essays the view reports {@code insufficient}. */
    public static final int MIN_ESSAYS = 3;
    static final int MAX_RECURRING_ERRORS = 10;
    private static final String CATCH_ALL_SUFFIX = "_OTHER";

    private final int fixedAfter;

    /** @param fixedAfter clean essays of the same task needed after the last occurrence before an error counts as fixed */
    public WritingProgressCalculator(int fixedAfter) {
        this.fixedAfter = fixedAfter;
    }

    /**
     * @param newestFirst the task's essays as the repository returns them (newest first)
     * @param texts       learner-facing error text by code, used only where a grade stored no name
     */
    public TaskProgress calculate(String task, List<WritingProgressEssay> newestFirst,
                                  Map<String, ErrorTypeTexts.Text> texts) {
        List<WritingProgressEssay> essays = new ArrayList<>(newestFirst);
        Collections.reverse(essays);
        Map<String, Tally> tallies = tally(essays);
        int lastIndex = essays.size() - 1;
        Map<String, ErrorTypeTexts.Text> lookup = texts == null ? Map.of() : texts;

        List<RecurringError> recurring = tallies.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, Tally> entry) -> catchAll(entry.getKey()) ? 1 : 0)
                        .thenComparingInt(entry -> -entry.getValue().essayIndexes.size())
                        .thenComparingInt(entry -> -severity(entry.getValue().latest)))
                .limit(MAX_RECURRING_ERRORS)
                .map(entry -> recurringError(entry.getKey(), entry.getValue(), essays.size(), lookup))
                .toList();

        List<FixedError> fixed = new ArrayList<>();
        for (Map.Entry<String, Tally> entry : tallies.entrySet()) {
            if (catchAll(entry.getKey())) continue;
            Tally tally = entry.getValue();
            int cleanSince = lastIndex - tally.lastIndex();
            if (cleanSince >= fixedAfter) {
                fixed.add(new FixedError(entry.getKey(), nameOf(entry.getKey(), tally.latest, lookup),
                        tally.lastSeen, cleanSince));
            }
        }
        fixed.sort(Comparator.comparingInt(FixedError::cleanEssaysSince).reversed());

        List<Point> points = essays.stream().map(WritingProgressCalculator::point).toList();
        return new TaskProgress(task, points, recurring, fixed, essays.size() < MIN_ESSAYS);
    }

    /** Oldest-first pass: each code records the essays it appears in and its latest correction. */
    private static Map<String, Tally> tally(List<WritingProgressEssay> chronological) {
        Map<String, Tally> tallies = new LinkedHashMap<>();
        for (int index = 0; index < chronological.size(); index++) {
            WritingProgressEssay essay = chronological.get(index);
            Set<String> countedInEssay = new HashSet<>();
            for (WritingGrade.Correction correction : essay.corrections()) {
                if (correction == null || correction.errorCode() == null) continue;
                Tally tally = tallies.computeIfAbsent(correction.errorCode(), code -> new Tally());
                tally.latest = correction;
                if (countedInEssay.add(correction.errorCode())) {
                    tally.essayIndexes.add(index);
                    tally.lastSeen = essay.gradedAt();
                }
            }
        }
        return tallies;
    }

    private static RecurringError recurringError(String code, Tally tally, int essaysTotal,
                                                 Map<String, ErrorTypeTexts.Text> texts) {
        return new RecurringError(code, nameOf(code, tally.latest, texts), fixHintOf(code, tally.latest, texts),
                tally.latest.practiceKnowledgePointId(), tally.latest.practiceKnowledgePointCode(),
                tally.essayIndexes.size(), essaysTotal, tally.lastSeen);
    }

    private static Point point(WritingProgressEssay essay) {
        Map<String, BigDecimal> criteria = new LinkedHashMap<>();
        for (WritingGrade.Criterion criterion : essay.criteria()) {
            if (criterion != null && criterion.code() != null && criterion.band() != null) {
                criteria.put(criterion.code(), criterion.band());
            }
        }
        return new Point(essay.submissionId(), sourceLabel(essay.source()), essay.gradedAt(), essay.overallBand(),
                criteria, essay.promptVersion());
    }

    private static String sourceLabel(String source) {
        if (source == null) return null;
        return switch (source) {
            case "LESSON_BLOCK" -> "LESSON";
            case "PRACTICE_ITEM" -> "PRACTICE";
            case "FREE_WRITING" -> "FREE";
            default -> source;
        };
    }

    private static String nameOf(String code, WritingGrade.Correction correction,
                                 Map<String, ErrorTypeTexts.Text> texts) {
        if (correction.errorName() != null) return correction.errorName();
        ErrorTypeTexts.Text text = texts.get(code);
        return text == null ? null : text.name();
    }

    private static String fixHintOf(String code, WritingGrade.Correction correction,
                                    Map<String, ErrorTypeTexts.Text> texts) {
        if (correction.fixHint() != null) return correction.fixHint();
        ErrorTypeTexts.Text text = texts.get(code);
        return text == null ? null : text.fixHint();
    }

    private static int severity(WritingGrade.Correction correction) {
        return correction.severity() == null ? 0 : correction.severity();
    }

    private static boolean catchAll(String code) {
        return code.endsWith(CATCH_ALL_SUFFIX);
    }

    /** Mutable working state for one error code; lives only inside {@link #calculate}. */
    private static final class Tally {
        private final List<Integer> essayIndexes = new ArrayList<>();
        private WritingGrade.Correction latest;
        private Instant lastSeen;

        private int lastIndex() {
            return essayIndexes.get(essayIndexes.size() - 1);
        }
    }
}
