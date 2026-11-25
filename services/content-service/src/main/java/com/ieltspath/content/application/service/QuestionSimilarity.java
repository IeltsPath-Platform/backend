package com.ieltspath.content.application.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class QuestionSimilarity {
    private static final Set<String> FUNCTION_WORDS = Set.of("a", "an", "the", "of", "is", "are", "was", "were", "to", "in", "on", "at", "for", "and", "or", "what", "which", "how");
    private QuestionSimilarity() {}
    public static double jaccard(String left, String right) {
        Set<String> a = words(left), b = words(right);
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> union = new java.util.HashSet<>(a); union.addAll(b);
        Set<String> intersection = new java.util.HashSet<>(a); intersection.retainAll(b);
        return (double) intersection.size() / union.size();
    }
    public static Set<String> words(String value) {
        String normalized = normalize(value);
        Set<String> all = Arrays.stream(normalized.split("\\s+")).filter(token -> !token.isBlank()).collect(Collectors.toSet());
        Set<String> meaningful = all.stream().filter(token -> !FUNCTION_WORDS.contains(token)).collect(Collectors.toSet());
        return meaningful.size() < 3 ? all : meaningful;
    }
    public static String normalize(String value) {
        if (value == null) return "";
        String text = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("_{3,}|\\[blank\\]", " blank ").replace('-', ' ');
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String tokens = decomposed.replaceAll("[^\\p{L}\\p{N}]+", " ").replaceAll("\\s+", " ").strip();
        return normalizeSmallNumbers(tokens);
    }

    private static String normalizeSmallNumbers(String text) {
        String[] tokens = text.isBlank() ? new String[0] : text.split(" ");
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < tokens.length) {
            int value = numberValue(tokens[index]);
            if (value >= 0) {
                int next = index + 1;
                int combined = value;
                if (next < tokens.length && tokens[next].equals("hundred") && value > 0 && value <= 1) {
                    combined = 100; next++;
                    if (next + 1 < tokens.length && tokens[next].equals("and") && numberValue(tokens[next + 1]) >= 0) {
                        combined += numberValue(tokens[next + 1]); next += 2;
                    }
                } else if (next < tokens.length && numberValue(tokens[next]) >= 0 && value >= 20 && numberValue(tokens[next]) < 10) {
                    combined += numberValue(tokens[next]); next++;
                } else if (next + 1 < tokens.length && tokens[next].equals("and") && numberValue(tokens[next + 1]) >= 0 && value >= 20) {
                    combined += numberValue(tokens[next + 1]); next += 2;
                }
                if (!result.isEmpty()) result.append(' ');
                result.append(combined);
                index = next;
            } else {
                if (!result.isEmpty()) result.append(' ');
                result.append(tokens[index++]);
            }
        }
        return result.toString();
    }

    private static int numberValue(String token) {
        return switch (token) {
            case "zero" -> 0; case "one" -> 1; case "two" -> 2; case "three" -> 3; case "four" -> 4;
            case "five" -> 5; case "six" -> 6; case "seven" -> 7; case "eight" -> 8; case "nine" -> 9;
            case "ten" -> 10; case "eleven" -> 11; case "twelve" -> 12; case "thirteen" -> 13; case "fourteen" -> 14;
            case "fifteen" -> 15; case "sixteen" -> 16; case "seventeen" -> 17; case "eighteen" -> 18; case "nineteen" -> 19;
            case "twenty" -> 20; case "thirty" -> 30; case "forty" -> 40; case "fifty" -> 50; case "sixty" -> 60;
            case "seventy" -> 70; case "eighty" -> 80; case "ninety" -> 90;
            default -> -1;
        };
    }
}
