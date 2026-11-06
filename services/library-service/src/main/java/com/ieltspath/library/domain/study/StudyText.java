package com.ieltspath.library.domain.study;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class StudyText {
    private StudyText() {}
    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
                .strip().toLowerCase(Locale.ROOT).replaceAll("(?U)\\s+", " ");
    }
    public static Pattern lemmaPattern(String lemma) {
        String quoted = String.join("\\s+", java.util.Arrays.stream(lemma.strip().split("(?U)\\s+"))
                .map(Pattern::quote).toList());
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + quoted + "(?![\\p{L}\\p{N}])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
