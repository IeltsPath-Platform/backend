package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.content.domain.vo.QuestionType;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Checks one model-written question before it can enter a draft. Returns a reason code, or null when the item is
 * usable. Only the shape and the answer key are checked here; the full answer-spec validation runs again when the
 * author approves, through the same use case that creates every other question.
 */
public final class QuestionDraftValidator {

    static final int MAX_STEM = 4000;
    static final int MAX_OPTIONS = 6;

    private QuestionDraftValidator() {
    }

    /** The answer kinds this slice generates: choice-style questions and typed answers. */
    public static boolean supports(QuestionType type) {
        return type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.TRUE_FALSE_NOT_GIVEN
                || type == QuestionType.FILL_IN_BLANK || type == QuestionType.SHORT_ANSWER;
    }

    /**
     * @param knownStems normalised stems (see {@link QuestionDraftItems#normalize}) of the questions the knowledge
     *                   point already has and of the items already kept; a stem among them is a duplicate
     */
    public static String rejectionReason(JsonNode item, QuestionType type, Set<String> activeErrorCodes,
                                  Set<String> knownStems) {
        if (item == null || !item.isObject()) {
            return "NOT_AN_OBJECT";
        }
        String stem = item.path("stem").asText("").trim();
        if (stem.isEmpty() || stem.length() > MAX_STEM) {
            return "BAD_STEM";
        }
        if (knownStems.contains(QuestionDraftItems.normalize(stem))) {
            return "DUPLICATE_STEM";
        }
        String explanation = item.path("explanation").asText("").trim();
        if (explanation.isEmpty()) {
            return "NO_EXPLANATION";
        }
        return isChoice(type) ? choiceReason(item, activeErrorCodes) : typedReason(item);
    }

    public static boolean isChoice(QuestionType type) {
        return type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.TRUE_FALSE_NOT_GIVEN;
    }

    private static String choiceReason(JsonNode item, Set<String> activeErrorCodes) {
        JsonNode options = item.path("options");
        if (!options.isArray() || options.size() < 2 || options.size() > MAX_OPTIONS) {
            return "BAD_OPTIONS";
        }
        Set<String> keys = new HashSet<>();
        for (JsonNode option : options) {
            String key = option.path("optionKey").asText("");
            if (key.isBlank() || !keys.add(key) || option.path("content").asText("").isBlank()) {
                return "BAD_OPTIONS";
            }
        }
        String correct = item.path("correct").asText("");
        if (!keys.contains(correct)) {
            return "CORRECT_NOT_AN_OPTION";
        }
        JsonNode distractors = item.path("distractors");
        Iterator<String> wrongKeys = distractors.fieldNames();
        while (wrongKeys.hasNext()) {
            String key = wrongKeys.next();
            if (!keys.contains(key) || key.equals(correct)) {
                return "DISTRACTOR_NOT_A_WRONG_OPTION";
            }
            String code = distractors.path(key).asText("");
            if (!activeErrorCodes.contains(code)) {
                return "UNKNOWN_ERROR_CODE";
            }
        }
        return null;
    }

    private static String typedReason(JsonNode item) {
        JsonNode accepted = item.path("accepted");
        if (!accepted.isArray() || accepted.isEmpty()) {
            return "NO_ACCEPTED_ANSWER";
        }
        for (JsonNode value : accepted) {
            if (!value.isTextual() || value.asText().isBlank()) {
                return "NO_ACCEPTED_ANSWER";
            }
        }
        return null;
    }
}
