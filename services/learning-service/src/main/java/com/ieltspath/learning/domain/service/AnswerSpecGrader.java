package com.ieltspath.learning.domain.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class AnswerSpecGrader {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Grade UNGRADABLE = new Grade(false, false, null);

    public Grade grade(Map<String, Object> spec, Object response) {
        String answer = extractAnswer(response);
        if (spec == null) {
            return UNGRADABLE;
        }

        Object type = spec.containsKey("type") ? spec.get("type") : "CHOICE";
        if ("CHOICE".equals(type)) {
            if (!(spec.get("correct") instanceof String correct) || correct.isBlank()) {
                return UNGRADABLE;
            }
            return new Grade(true, answer != null && !answer.isBlank() && correct.equals(answer), correct);
        }
        if ("FILL".equals(type)) {
            if (!(spec.get("accepted") instanceof List<?> accepted) || accepted.isEmpty()) {
                return UNGRADABLE;
            }
            for (Object value : accepted) {
                if (!(value instanceof String text) || normalize(text).isEmpty()) {
                    return UNGRADABLE;
                }
            }
            String normalizedAnswer = answer == null ? "" : normalize(answer);
            boolean correct = !normalizedAnswer.isEmpty()
                    && accepted.stream().anyMatch(value -> normalize((String) value).equals(normalizedAnswer));
            return new Grade(true, correct, (String) accepted.getFirst());
        }
        return UNGRADABLE;
    }

    private static String extractAnswer(Object response) {
        Object answer = response instanceof Map<?, ?> payload ? payload.get("answer") : response;
        if (answer == null || answer instanceof String) {
            return (String) answer;
        }
        throw new IllegalArgumentException("Answer must be a string or null.");
    }

    private static String normalize(String value) {
        return WHITESPACE.matcher(value).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }

    public record Grade(boolean gradable, boolean correct, String correctAnswer) {
    }
}
