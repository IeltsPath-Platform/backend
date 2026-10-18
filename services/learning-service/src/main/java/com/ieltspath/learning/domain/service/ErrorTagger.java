package com.ieltspath.learning.domain.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Names the error behind a wrong objective answer, using the error-tag rules of answer spec v1. A CHOICE answer takes
 * its code from {@code distractors}. A FILL answer is classified only when it is wrong: word limit first, then a
 * number in another value or format, then a plural mismatch, then a spelling slip of at most two edits. Anything else,
 * and any correct or omitted answer, has no tag.
 */
public final class ErrorTagger {
    private static final Pattern NUMBER = Pattern.compile("[0-9][0-9:.,]*");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final int MAX_SPELLING_EDITS = 2;

    public String tag(Map<String, Object> spec, Object response, boolean correct) {
        if (correct || spec == null) {
            return null;
        }
        String answer = answerOf(response);
        if (answer == null || answer.isBlank()) {
            return null;
        }
        Object type = spec.containsKey("type") ? spec.get("type") : "CHOICE";
        if ("CHOICE".equals(type)) {
            return distractorCode(spec, answer);
        }
        if ("FILL".equals(type)) {
            return tagFill(spec, answer);
        }
        return null;
    }

    private static String distractorCode(Map<String, Object> spec, String key) {
        if (!(spec.get("distractors") instanceof Map<?, ?> distractors)) {
            return null;
        }
        Object code = distractors.get(key);
        return code instanceof String text && !text.isBlank() ? text : null;
    }

    private static String tagFill(Map<String, Object> spec, String answer) {
        if (!(spec.get("accepted") instanceof List<?> accepted) || accepted.isEmpty()) {
            return null;
        }
        if (spec.get("maxWords") instanceof Number maxWords && wordCount(answer) > maxWords.intValue()) {
            return "WORD_LIMIT_EXCEEDED";
        }
        String normalized = normalize(answer);
        List<String> expected = accepted.stream().filter(String.class::isInstance)
                .map(value -> normalize((String) value)).toList();
        if (expected.isEmpty()) {
            return null;
        }
        if (NUMBER.matcher(normalized).matches() && expected.stream().anyMatch(value -> NUMBER.matcher(value).matches())) {
            return "NUMBER_FORMAT";
        }
        if (expected.stream().anyMatch(value -> normalized.equals(value + "s") || value.equals(normalized + "s"))) {
            return "PLURAL_FORM";
        }
        boolean spelling = expected.stream().anyMatch(value -> !NUMBER.matcher(value).matches()
                && levenshtein(normalized, value) <= MAX_SPELLING_EDITS);
        return spelling ? "SPELLING" : null;
    }

    /** The {@code answer} of a parsed payload map, or the raw string when the response is not a map. */
    static String answerOf(Object response) {
        Object answer = response instanceof Map<?, ?> payload ? payload.get("answer") : response;
        return answer instanceof String text ? text : null;
    }

    private static String normalize(String value) {
        return WHITESPACE.matcher(value).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }

    private static long wordCount(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? 0 : WHITESPACE.split(trimmed).length;
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1];
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            previous = current;
        }
        return previous[right.length()];
    }
}
