package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.WritingBandCaps.Density;
import com.ieltspath.learning.domain.vo.WritingGrade;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * Counts how many sentences of an essay carry an error. Pure: no Spring, no clock, no I/O.
 *
 * <p>The count comes from the corrections, not from the model's own estimate, so the same essay and corrections always
 * give the same numbers. A correction that is only a suggestion of a better wording is not an error and is not counted.
 */
public final class WritingErrorDensity {
    /** Codes counted as vocabulary errors although their category is grammar: wrong word class and wrong plural. */
    private static final Set<String> WORD_LEVEL_CODES = Set.of("WORD_FORM", "NOUN_PLURAL", "WRITING_SPELLING");

    private WritingErrorDensity() {}

    /** The sentence and error counts of {@code essay}; {@code task1Checks} is null for Task 2 and for older templates. */
    public static Density measure(String essay, List<WritingGrade.Correction> corrections,
                                  WritingGrade.Task1Checks task1Checks) {
        List<int[]> sentences = sentences(essay);
        BitSet grammar = new BitSet(sentences.size());
        BitSet vocabulary = new BitSet(sentences.size());
        BitSet coherence = new BitSet(sentences.size());
        BitSet impeding = new BitSet(sentences.size());
        for (WritingGrade.Correction correction : corrections) {
            if (WritingGrade.Correction.SUGGESTION.equals(correction.certainty()) || correction.excerpt() == null) continue;
            int start = essay.indexOf(correction.excerpt());
            if (start < 0) continue;
            int sentence = sentenceAt(sentences, start);
            if (sentence < 0) continue;
            if ("GRAMMAR".equals(correction.category())) {
                grammar.set(sentence);
                if (correction.impeding()) impeding.set(sentence);
            }
            if ("VOCABULARY".equals(correction.category()) || WORD_LEVEL_CODES.contains(correction.errorCode())) {
                vocabulary.set(sentence);
            }
            if ("COHERENCE".equals(correction.category())) coherence.set(sentence);
        }
        int wrong = 0;
        int right = 0;
        boolean overviewWrong = false;
        if (task1Checks != null) {
            for (WritingGrade.DataCheck check : task1Checks.dataChecks()) {
                if (WritingGrade.DataCheck.WRONG.equals(check.verdict())) wrong++;
                if (WritingGrade.DataCheck.CORRECT.equals(check.verdict())) right++;
            }
            overviewWrong = Boolean.FALSE.equals(task1Checks.overviewAccurate());
        }
        return new Density(sentences.size(), grammar.cardinality(), vocabulary.cardinality(), coherence.cardinality(),
                impeding.cardinality(), wrong + right, wrong, overviewWrong);
    }

    /** Sentence spans as {start, end}: a sentence ends after . ! or ? followed by a space, or at a line break. */
    static List<int[]> sentences(String essay) {
        List<int[]> spans = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < essay.length(); i++) {
            char c = essay.charAt(i);
            boolean ends = c == '\n' || ((c == '.' || c == '!' || c == '?') && (i + 1 == essay.length()
                    || Character.isWhitespace(essay.charAt(i + 1))));
            if (ends) {
                addIfText(spans, essay, start, i + 1);
                start = i + 1;
            }
        }
        addIfText(spans, essay, start, essay.length());
        return spans;
    }

    private static void addIfText(List<int[]> spans, String essay, int start, int end) {
        for (int i = start; i < end; i++) {
            if (Character.isLetterOrDigit(essay.charAt(i))) {
                spans.add(new int[] {start, end});
                return;
            }
        }
    }

    private static int sentenceAt(List<int[]> spans, int position) {
        for (int i = 0; i < spans.size(); i++) {
            if (position >= spans.get(i)[0] && position < spans.get(i)[1]) return i;
        }
        return -1;
    }
}
