package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.learning.domain.vo.WritingGrade.DataCheck;
import com.ieltspath.learning.domain.vo.WritingGrade.OutlineItem;
import com.ieltspath.learning.domain.vo.WritingGrade.Position;
import com.ieltspath.learning.domain.vo.WritingGrade.QuestionPart;
import com.ieltspath.learning.domain.vo.WritingGrade.Task1Checks;
import com.ieltspath.learning.domain.vo.WritingGrade.Task2Checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads the whole-essay checks the second prompt template asks for. Pure: nothing here calls the model or a service.
 *
 * <p>The model is trusted for nothing it cannot back with the essay: a quotation that is not an exact part of the
 * essay is dropped, a field of the wrong type reads as unknown, and a missing or malformed object reads as empty. None
 * of that fails the grade, because these checks only add to what the learner sees.
 */
final class TaskChecksParser {
    static final int MAX_DATA_CHECKS = 20;
    static final int MAX_QUESTION_PARTS = 4;
    static final int MAX_OUTLINE_ITEMS = 8;
    static final int MAX_TEXT_CHARS = 200;

    private static final Set<String> VERDICTS = Set.of(DataCheck.CORRECT, DataCheck.WRONG, DataCheck.UNVERIFIABLE);
    private static final Set<String> ADDRESSED = Set.of("FULL", "PARTIAL", "NONE");
    private static final Set<String> ROLES = Set.of("INTRODUCTION", "BODY", "CONCLUSION");

    private TaskChecksParser() {}

    static Task1Checks task1(JsonNode checks, String essay) {
        if (checks == null || !checks.isObject()) return new Task1Checks(List.of(), 0, null, null, null, null);
        List<DataCheck> dataChecks = new ArrayList<>();
        JsonNode items = checks.path("dataChecks");
        if (items.isArray()) {
            for (JsonNode item : items) {
                if (dataChecks.size() == MAX_DATA_CHECKS) break;
                String excerpt = quotation(item.path("excerpt"), essay);
                String verdict = item.path("verdict").asText("");
                if (excerpt == null || !VERDICTS.contains(verdict)) continue;
                String expected = DataCheck.WRONG.equals(verdict) ? text(item.path("expected")) : null;
                dataChecks.add(new DataCheck(excerpt, verdict, expected));
            }
        }
        int correct = (int) dataChecks.stream().filter(check -> DataCheck.CORRECT.equals(check.verdict())).count();
        String opinionExcerpt = quotation(checks.path("personalOpinionExcerpt"), essay);
        Boolean opinion = flag(checks.path("hasPersonalOpinion"));
        // An opinion that is claimed but cannot be quoted is not kept: it would blame the essay for nothing.
        if (Boolean.TRUE.equals(opinion) && opinionExcerpt == null) opinion = null;
        if (!Boolean.TRUE.equals(opinion)) opinionExcerpt = null;
        return new Task1Checks(dataChecks, correct, flag(checks.path("hasOverview")),
                flag(checks.path("overviewAccurate")), opinion, opinionExcerpt);
    }

    static Task2Checks task2(JsonNode checks, String essay) {
        if (checks == null || !checks.isObject()) return new Task2Checks(List.of(), null, List.of());
        return new Task2Checks(questionParts(checks.path("questionParts"), essay), position(checks.path("position"), essay),
                outline(checks.path("outline"), essay));
    }

    private static List<QuestionPart> questionParts(JsonNode items, String essay) {
        List<QuestionPart> parts = new ArrayList<>();
        if (!items.isArray()) return parts;
        for (JsonNode item : items) {
            if (parts.size() == MAX_QUESTION_PARTS) break;
            String part = text(item.path("part"));
            String addressed = item.path("addressed").asText("");
            if (part == null || !ADDRESSED.contains(addressed)) continue;
            String excerpt = quotation(item.path("excerpt"), essay);
            // An answered part is a claim about the essay, so it needs a quotation to stand on; NONE has none to give.
            if (!"NONE".equals(addressed) && excerpt == null) continue;
            parts.add(new QuestionPart(part, addressed, "NONE".equals(addressed) ? null : excerpt));
        }
        return parts;
    }

    private static Position position(JsonNode node, String essay) {
        if (!node.isObject() || !node.path("required").isBoolean() || !node.path("stated").isBoolean()) return null;
        Boolean consistent = flag(node.path("consistent"));
        String conflict = Boolean.FALSE.equals(consistent) ? quotation(node.path("conflictExcerpt"), essay) : null;
        return new Position(node.get("required").asBoolean(), node.get("stated").asBoolean(),
                quotation(node.path("excerpt"), essay), consistent, conflict);
    }

    private static List<OutlineItem> outline(JsonNode items, String essay) {
        List<OutlineItem> outline = new ArrayList<>();
        if (!items.isArray()) return outline;
        for (JsonNode item : items) {
            if (outline.size() == MAX_OUTLINE_ITEMS) break;
            String role = item.path("role").asText("");
            String idea = text(item.path("mainIdea"));
            if (!ROLES.contains(role) || idea == null || !item.path("developed").isBoolean()) continue;
            outline.add(new OutlineItem(role, idea, item.get("developed").asBoolean(), quotation(item.path("excerpt"), essay)));
        }
        return outline;
    }

    /** The text when it is a non-blank string, cut to the length limit; otherwise null. */
    private static String text(JsonNode node) {
        if (!node.isTextual() || node.asText().isBlank()) return null;
        String value = node.asText().trim();
        return value.length() <= MAX_TEXT_CHARS ? value : value.substring(0, MAX_TEXT_CHARS);
    }

    /** The text when it is an exact part of the essay; otherwise null. */
    private static String quotation(JsonNode node, String essay) {
        if (!node.isTextual() || node.asText().isBlank() || !essay.contains(node.asText())) return null;
        String value = node.asText();
        return value.length() <= MAX_TEXT_CHARS ? value : value.substring(0, MAX_TEXT_CHARS);
    }

    private static Boolean flag(JsonNode node) {
        return node.isBoolean() ? node.asBoolean() : null;
    }
}
