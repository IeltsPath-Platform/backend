package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.content.domain.vo.QuestionType;
import java.util.List;
import java.util.Set;

public final class SectionQuestionChecks {
    private SectionQuestionChecks() {}
    public static String reason(JsonNode item, QuestionType type, String source, List<String> existing,
                                List<String> kept, double duplicateThreshold, double sameAnswerThreshold,
                                int maxAnswerWords, int maxCopiedWords, int maxStemChars) {
        String base = QuestionDraftValidator.rejectionReason(item, type, Set.of(), Set.of());
        if (base != null && !"UNKNOWN_ERROR_CODE".equals(base)) return base;
        String stem = item.path("stem").asText("").strip();
        if (stem.length() > maxStemChars) return "BAD_STEM";
        if (type == QuestionType.TRUE_FALSE_NOT_GIVEN) {
            if (item.path("options").size() != 3) return "BAD_TFNG_OPTIONS";
            Set<String> keys = new java.util.HashSet<>();
            item.path("options").forEach(option -> keys.add(option.path("optionKey").asText()));
            if (!keys.equals(Set.of("TRUE", "FALSE", "NOT_GIVEN"))) return "BAD_TFNG_OPTIONS";
        }
        if (QuestionDraftValidator.isChoice(type)) {
            Set<String> options = new java.util.HashSet<>();
            item.path("options").forEach(option -> options.add(QuestionSimilarity.normalize(option.path("content").asText())));
            if (options.size() != item.path("options").size()) return "DUPLICATE_OPTIONS";
        } else {
            JsonNode answers = item.path("accepted");
            Set<String> unique = new java.util.HashSet<>();
            for (JsonNode answer : answers) {
                String value = answer.asText();
                if (value.split("\\s+").length > maxAnswerWords) return "ANSWER_TOO_LONG";
                if (!unique.add(QuestionSimilarity.normalize(value))) return "DUPLICATE_ANSWER";
            }
            boolean inSource = false;
            for (JsonNode answer : answers) {
                if (containsWords(source, answer.asText())) { inSource = true; break; }
            }
            if (!inSource) return "ANSWER_NOT_IN_SOURCE";
            for (JsonNode answer : answers) {
                String normalized = QuestionSimilarity.normalize(answer.asText());
                if (normalized.length() >= 4 && containsWords(stem, answer.asText())) return "ANSWER_IN_STEM";
            }
            long blanks = stem.split("_{3,}|(?i)\\[blank\\]", -1).length - 1;
            if (type == QuestionType.FILL_IN_BLANK && blanks != 1) return "BAD_BLANK";
        }
        if (longestCommonWords(stem, source) > maxCopiedWords) return "COPIED_FROM_SOURCE";
        for (int i = 0; i < existing.size(); i++) {
            String previous = existing.get(i);
            if (similar(stem, previous, duplicateThreshold, sameAnswerThreshold, type, item)) return "DUPLICATE_IN_SECTION:" + (i + 1);
        }
        for (String previous : kept) {
            if (QuestionSimilarity.jaccard(stem, previous) >= duplicateThreshold) return "DUPLICATE_IN_DRAFT";
        }
        return null;
    }
    private static boolean similar(String a, String b, double threshold, double sameAnswerThreshold, QuestionType type, JsonNode item) {
        return QuestionSimilarity.jaccard(a, b) >= threshold;
    }
    private static boolean containsWords(String text, String phrase) {
        String source = " " + QuestionSimilarity.normalize(text) + " ";
        String answer = " " + QuestionSimilarity.normalize(phrase) + " ";
        return answer.length() > 2 && source.contains(answer);
    }
    private static int longestCommonWords(String left, String right) {
        String[] a = QuestionSimilarity.normalize(left).split("\\s+"), b = QuestionSimilarity.normalize(right).split("\\s+");
        int max = 0;
        for (int i = 0; i < a.length; i++) for (int j = 0; j < b.length; j++) {
            int count = 0; while (i + count < a.length && j + count < b.length && a[i + count].equals(b[j + count])) count++;
            max = Math.max(max, count);
        }
        return max;
    }
}
