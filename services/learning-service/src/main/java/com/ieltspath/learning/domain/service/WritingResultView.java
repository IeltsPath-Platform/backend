package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingGrade.Correction;
import com.ieltspath.learning.domain.vo.WritingGrade.ErrorExample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the result screen shows, worked out from a stored grade. Pure: no Spring, no clock, no I/O, so reading a
 * graded essay never calls Content or the model.
 */
public final class WritingResultView {
    static final int MAX_PRIORITIES = 3;
    static final String FALLBACK_SUFFIX = "_OTHER";

    private WritingResultView() {}

    /** Character range {@code [start, end)} of a quotation in the essay. */
    public record Span(int start, int end) {}

    /** The errors of one kind: how many, how serious, the rule, a similar example, and where to practise. */
    public record ErrorGroup(String errorCode, String errorName, String category, int count, int severity,
                             String fixHint, ErrorExample example, UUID practiceKnowledgePointId) {}

    /** One of the few problems to work on first. {@code source} is AI when the model chose it, RULE when it was derived. */
    public record PriorityItem(String title, String explanation, List<String> excerpts, String action,
                               List<String> errorCodes, UUID practiceKnowledgePointId, String source) {}

    /**
     * Where each correction's quotation sits in the essay, in the order of the corrections. A quotation takes the first
     * occurrence that no earlier correction with the same quotation has taken; one with no free occurrence has no
     * position (null). Overlapping quotations of different corrections each keep their own position.
     */
    public static List<Span> locate(String essay, List<Correction> corrections) {
        List<Span> spans = new ArrayList<>();
        Set<Span> taken = new HashSet<>();
        for (Correction correction : corrections) {
            String excerpt = correction.excerpt();
            Span span = null;
            if (essay != null && excerpt != null && !excerpt.isEmpty()) {
                for (int at = essay.indexOf(excerpt); at >= 0; at = essay.indexOf(excerpt, at + 1)) {
                    Span candidate = new Span(at, at + excerpt.length());
                    if (taken.add(candidate)) {
                        span = candidate;
                        break;
                    }
                }
            }
            spans.add(span);
        }
        return java.util.Collections.unmodifiableList(spans); // null marks a quotation with no position
    }

    /**
     * The errors grouped by code, worst first by {@code count x severity}. Suggestions are not errors and do not count.
     * Groups of fallback codes go last; ties keep the order in which the codes first appear.
     */
    public static List<ErrorGroup> errorGroups(List<Correction> corrections, List<ErrorExample> examples) {
        Map<String, List<Correction>> byCode = new LinkedHashMap<>();
        for (Correction correction : corrections) {
            if (correction.errorCode() == null || correction.isSuggestion()) continue;
            byCode.computeIfAbsent(correction.errorCode(), code -> new ArrayList<>()).add(correction);
        }
        List<ErrorGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Correction>> entry : byCode.entrySet()) {
            List<Correction> members = entry.getValue();
            Correction first = members.getFirst();
            UUID practice = members.stream().map(Correction::practiceKnowledgePointId)
                    .filter(java.util.Objects::nonNull).findFirst().orElse(null);
            ErrorExample example = examples.stream().filter(item -> entry.getKey().equals(item.errorCode()))
                    .findFirst().orElse(null);
            groups.add(new ErrorGroup(entry.getKey(), first.errorName(), first.category(), members.size(),
                    first.severity() == null ? 1 : first.severity(), first.fixHint(), example, practice));
        }
        List<ErrorGroup> ordered = new ArrayList<>(groups);
        ordered.sort((left, right) -> {
            boolean leftFallback = left.errorCode().endsWith(FALLBACK_SUFFIX);
            boolean rightFallback = right.errorCode().endsWith(FALLBACK_SUFFIX);
            if (leftFallback != rightFallback) return leftFallback ? 1 : -1;
            return Integer.compare(right.count() * right.severity(), left.count() * left.severity());
        });
        return List.copyOf(ordered);
    }

    /**
     * The problems to work on first. The model's choice comes first, at most three; when it gave none, the three
     * worst error groups stand in for it. An essay with no errors has none.
     */
    public static List<PriorityItem> priorities(WritingGrade grade) {
        if (!grade.priorities().isEmpty()) {
            return grade.priorities().stream().limit(MAX_PRIORITIES)
                    .map(priority -> new PriorityItem(priority.title(), priority.explanation(), priority.excerpts(),
                            priority.action(), priority.errorCodes(), practiceFor(grade, priority.errorCodes()), "AI"))
                    .toList();
        }
        return errorGroups(grade.corrections(), grade.errorExamples()).stream().limit(MAX_PRIORITIES).map(group -> {
            Correction first = grade.corrections().stream().filter(item -> group.errorCode().equals(item.errorCode())
                    && !item.isSuggestion()).findFirst().orElseThrow();
            String explanation = first.reason() != null && !first.reason().isBlank() ? first.reason() : group.fixHint();
            return new PriorityItem(group.errorName(), explanation, List.of(first.excerpt()), group.fixHint(),
                    List.of(group.errorCode()), group.practiceKnowledgePointId(), "RULE");
        }).toList();
    }

    private static UUID practiceFor(WritingGrade grade, List<String> errorCodes) {
        return grade.corrections().stream().filter(item -> errorCodes.contains(item.errorCode()))
                .map(Correction::practiceKnowledgePointId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }
}
