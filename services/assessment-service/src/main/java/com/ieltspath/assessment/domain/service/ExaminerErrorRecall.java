package com.ieltspath.assessment.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Whether the model's corrections cover the errors an examiner marked. Pure: no clock, no I/O.
 *
 * <p>A marked error is captured when a model correction overlaps any occurrence of its excerpt in the essay. The
 * code the model gave is not compared: the question here is whether it pointed at the same words.
 */
public final class ExaminerErrorRecall {
    private ExaminerErrorRecall() {}

    public enum Kind { LANGUAGE, DATA }

    /** An examiner-marked error; {@code excerpt} should be an exact substring of the essay. */
    public record MarkedError(String excerpt, Kind kind) {}

    /** Counts for one essay or summed over several; a total of zero means no recall rate can be stated. */
    public record Counts(int languageTotal, int languageCaptured, int dataTotal, int dataCaptured) {
        public static final Counts NONE = new Counts(0, 0, 0, 0);

        public Counts plus(Counts other) {
            return new Counts(languageTotal + other.languageTotal, languageCaptured + other.languageCaptured,
                    dataTotal + other.dataTotal, dataCaptured + other.dataCaptured);
        }

        /** Captured over marked for language errors, or null when none were marked. */
        public BigDecimal languageRecall() {
            return rate(languageCaptured, languageTotal);
        }

        /** Captured over marked for data errors, or null when none were marked. */
        public BigDecimal dataRecall() {
            return rate(dataCaptured, dataTotal);
        }

        private static BigDecimal rate(int captured, int total) {
            if (total == 0) return null;
            return BigDecimal.valueOf(captured).divide(BigDecimal.valueOf(total), 5, RoundingMode.HALF_UP);
        }
    }

    /**
     * @param essay            the essay the errors and corrections refer to
     * @param marked           errors the examiner marked; an excerpt not found in the essay is not counted
     * @param correctionTexts  the excerpts of the model's corrections; ones not found in the essay are ignored
     */
    public static Counts score(String essay, List<MarkedError> marked, List<String> correctionTexts) {
        if (essay == null || marked == null || marked.isEmpty()) return Counts.NONE;
        List<int[]> corrected = spans(essay, correctionTexts == null ? List.of() : correctionTexts);
        int languageTotal = 0, languageCaptured = 0, dataTotal = 0, dataCaptured = 0;
        for (MarkedError error : marked) {
            List<int[]> occurrences = occurrences(essay, error.excerpt());
            if (occurrences.isEmpty()) continue;
            boolean captured = occurrences.stream().anyMatch(span -> corrected.stream().anyMatch(other -> overlaps(span, other)));
            if (error.kind() == Kind.DATA) {
                dataTotal++;
                if (captured) dataCaptured++;
            } else {
                languageTotal++;
                if (captured) languageCaptured++;
            }
        }
        return new Counts(languageTotal, languageCaptured, dataTotal, dataCaptured);
    }

    private static List<int[]> spans(String essay, List<String> texts) {
        List<int[]> spans = new ArrayList<>();
        for (String text : texts) spans.addAll(occurrences(essay, text));
        return spans;
    }

    /** Every start position of {@code excerpt} in {@code essay}, as half-open {start, end} spans. */
    static List<int[]> occurrences(String essay, String excerpt) {
        List<int[]> found = new ArrayList<>();
        if (excerpt == null || excerpt.isBlank()) return found;
        int index = essay.indexOf(excerpt);
        while (index >= 0) {
            found.add(new int[]{index, index + excerpt.length()});
            index = essay.indexOf(excerpt, index + 1);
        }
        return found;
    }

    private static boolean overlaps(int[] a, int[] b) {
        return a[0] < b[1] && b[0] < a[1];
    }
}
