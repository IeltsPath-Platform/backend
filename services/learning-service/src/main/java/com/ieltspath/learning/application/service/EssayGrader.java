package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.exception.WritingGradingException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.EssayPromptActivationProvider;
import com.ieltspath.learning.application.port.LearningContentClient.ErrorType;
import com.ieltspath.learning.application.port.LearningContentClient.Rubric;
import com.ieltspath.learning.application.port.LearningContentClient.RubricCriterion;
import com.ieltspath.learning.application.port.LlmClient;
import com.ieltspath.learning.domain.service.WritingScore;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingTask;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Uses Content's active rubric during bootstrap, then the task's activated immutable rubric version. */
@Component
public class EssayGrader {
    static final int MAX_ITEM_CHARS = 300;
    static final int MAX_FOCUS_POINTS = 4;
    static final int MAX_FOCUS_TITLE_CHARS = 80;
    static final int MAX_CORRECTIONS = 10;
    static final int MAX_SUMMARY_CHARS = 600;
    static final String TEMPLATE_VERSION = "v1";
    static final String PROMPT_TEMPLATE = loadTemplate();

    private final LlmClient llm;
    private final ObjectMapper json;
    private final LearningContentClient content;
    private final EssayPromptActivationProvider activations;

    public EssayGrader(LlmClient llm, ObjectMapper json, LearningContentClient content,
                       EssayPromptActivationProvider activations) {
        this.llm = llm;
        this.json = json;
        this.content = content;
        this.activations = activations;
    }

    public boolean available() { return llm.configured(); }

    public GradedEssay grade(EssayPrompt prompt, String essay) {
        WritingTask task = WritingTask.parse(prompt.task())
                .orElseThrow(() -> new WritingGradingException("INVALID_PROMPT"));
        if (task == WritingTask.TASK_1 && (prompt.chartFacts() == null || prompt.chartFacts().isBlank())) {
            throw new WritingGradingException("INVALID_PROMPT");
        }
        Rubric rubric;
        List<ErrorType> errorTypes;
        String expectedVersion;
        try {
            expectedVersion = activations.activatedVersion(task.name());
            rubric = resolveRubric(task, expectedVersion);
            errorTypes = content.activeErrorTypes();
        } catch (LearningRequestException exception) {
            throw new WritingGradingException("RUBRIC_UNAVAILABLE");
        }
        if (rubric == null || rubric.criteria() == null || rubric.criteria().isEmpty()
                || rubric.code() == null || rubric.version() < 1 || !task.name().equals(rubric.task())
                || errorTypes == null) {
            throw new WritingGradingException("RUBRIC_UNAVAILABLE");
        }
        String promptVersion = rubric.code() + "@" + rubric.version() + "+tpl" + TEMPLATE_VERSION;
        if (expectedVersion != null && !expectedVersion.equals(promptVersion)) {
            throw new WritingGradingException("PROMPT_VERSION_UNAVAILABLE");
        }

        Map<String, ErrorType> allowedCodes = errorTypes.stream().filter(Objects::nonNull)
                .filter(error -> error.code() != null).collect(Collectors.toMap(ErrorType::code, error -> error,
                        (first, ignored) -> first));
        Map<String, UUID> criterionPoints = new HashMap<>();
        for (RubricCriterion criterion : rubric.criteria()) {
            if (criterion != null && criterion.code() != null && criterion.knowledgePointId() != null) {
                criterionPoints.putIfAbsent(criterion.code(), criterion.knowledgePointId());
            }
        }
        String system = systemPrompt(task, rubric, errorTypes);
        String user = userPrompt(task, prompt, essay);
        try {
            Optional<WritingGrade> first = parse(task, essay, allowedCodes, criterionPoints,
                    llm.completeJson(system, user, false));
            WritingGrade grade = first.orElseGet(() -> {
                try {
                    return parse(task, essay, allowedCodes, criterionPoints, llm.completeJson(system, user, true))
                            .orElseThrow(() -> new WritingGradingException("INVALID_GRADE"));
                } catch (LlmUnavailableException exception) {
                    throw new WritingGradingException("LLM_UNAVAILABLE");
                }
            });
            return new GradedEssay(withCriterionStudyPoints(grade, rubric, prompt.passBand()), promptVersion);
        } catch (LlmUnavailableException exception) {
            throw new WritingGradingException("LLM_UNAVAILABLE");
        }
    }

    private Rubric resolveRubric(WritingTask task, String expectedVersion) {
        if (expectedVersion == null) return content.getActiveRubric(task.name());
        String[] parts = expectedVersion.split("@|\\+tpl", -1);
        if (parts.length != 3 || parts[0].isBlank() || !TEMPLATE_VERSION.equals(parts[2])) {
            throw new WritingGradingException("PROMPT_VERSION_UNAVAILABLE");
        }
        try {
            int version = Integer.parseInt(parts[1]);
            if (version < 1) throw new NumberFormatException();
            return content.getRubricVersion(parts[0], version);
        } catch (NumberFormatException exception) {
            throw new WritingGradingException("PROMPT_VERSION_UNAVAILABLE");
        }
    }

    static String systemPrompt(WritingTask task, Rubric rubric, List<ErrorType> errorTypes) {
        String criterionText = rubric.criteria().stream().sorted(Comparator.comparingInt(RubricCriterion::sortOrder))
                .map(EssayGrader::renderCriterion).collect(Collectors.joining("\n\n"));
        String errorCodes = errorTypes.stream().filter(Objects::nonNull).filter(error -> error.code() != null)
                .sorted(Comparator.comparing(ErrorType::code))
                .map(error -> error.code() + " — " + error.name() + ": " + error.fixHint())
                .collect(Collectors.joining("\n"));
        String taskName = task == WritingTask.TASK_1 ? "Academic Writing Task 1" : "Writing Task 2";
        return PROMPT_TEMPLATE.replace("{{task}}", taskName)
                .replace("{{criteria}}", criterionText)
                .replace("{{error_codes}}", errorCodes.isBlank() ? "(none)" : errorCodes);
    }

    private static String renderCriterion(RubricCriterion criterion) {
        String descriptors = criterion.bandDescriptors() == null ? "" : criterion.bandDescriptors().stream()
                .sorted(Comparator.comparing(LearningContentClient.RubricBandDescriptor::band))
                .map(item -> "  Band " + item.band().stripTrailingZeros().toPlainString() + ": " + item.descriptor())
                .collect(Collectors.joining("\n"));
        return criterion.code() + " (" + criterion.name() + "): " + criterion.description()
                + (descriptors.isBlank() ? "" : "\n" + descriptors);
    }

    static String userPrompt(WritingTask task, EssayPrompt prompt, String essay) {
        StringBuilder text = new StringBuilder().append("Question:\n")
                .append(prompt.stem() == null ? "" : prompt.stem()).append("\n\n")
                .append("Minimum words: ").append(prompt.minWords() == null ? "" : prompt.minWords()).append('\n')
                .append("Word count of the essay: ").append(WritingScore.countWords(essay)).append("\n\n");
        if (task == WritingTask.TASK_1) {
            text.append("<chart_facts>\n").append(neutralize(prompt.chartFacts(), "chart_facts"))
                    .append("\n</chart_facts>\n\n");
        }
        return text.append("<essay>\n").append(neutralize(essay, "essay")).append("\n</essay>").toString();
    }

    private static String neutralize(String value, String tag) {
        return value.replaceAll("(?i)</\\s*" + tag + "\\s*>", "[/" + tag + "]");
    }

    private Optional<WritingGrade> parse(WritingTask task, String essay, Map<String, ErrorType> allowedCodes,
                                         Map<String, UUID> criterionPoints, String reply) {
        JsonNode root = readObject(reply);
        if (root == null || !root.path("criteria").isArray()) return Optional.empty();
        Map<String, WritingGrade.Criterion> byCode = new LinkedHashMap<>();
        for (JsonNode node : root.get("criteria")) {
            String code = node.path("code").asText(null);
            JsonNode band = node.path("band");
            if (code == null || !task.criteria().contains(code) || byCode.containsKey(code) || !band.isNumber()) {
                return Optional.empty();
            }
            BigDecimal value = band.decimalValue();
            if (!WritingScore.isBand(value)) return Optional.empty();
            String comment = truncate(node.path("comment").asText(""), MAX_ITEM_CHARS);
            byCode.put(code, new WritingGrade.Criterion(code, value.setScale(1), List.of(),
                    comment.isBlank() ? List.of() : List.of(comment), criterionPoints.get(code)));
        }
        if (byCode.size() != task.criteria().size()) return Optional.empty();
        List<WritingGrade.Criterion> criteria = task.criteria().stream().map(byCode::get).toList();
        String summary = truncate(root.path("summary").asText(""), MAX_SUMMARY_CHARS);
        return Optional.of(new WritingGrade(criteria, corrections(root.path("corrections"), essay, allowedCodes),
                summary, WritingScore.overallBand(criteria.stream().map(WritingGrade.Criterion::band).toList()),
                focusPoints(root.path("focus"))));
    }

    /** Focus entries are "<what to study>: <why>"; anything that is not a non-blank string is dropped. */
    private static List<WritingGrade.StudyPoint> focusPoints(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<WritingGrade.StudyPoint> points = new ArrayList<>();
        for (JsonNode item : node) {
            if (points.size() == MAX_FOCUS_POINTS) break;
            if (!item.isTextual()) continue;
            String text = truncate(item.asText().trim(), MAX_ITEM_CHARS);
            int colon = text.indexOf(':');
            String title = (colon < 0 ? text : text.substring(0, colon)).trim();
            String reason = colon < 0 ? "" : text.substring(colon + 1).trim();
            if (title.isBlank()) continue;
            points.add(new WritingGrade.StudyPoint(WritingGrade.StudyPoint.AI, null, null, null, null, null,
                    truncate(title, MAX_FOCUS_TITLE_CHARS), reason.isBlank() ? null : reason));
        }
        return points;
    }

    /** Weak criteria with knowledge points come first, weakest first, followed by the model's own focus. */
    private WritingGrade withCriterionStudyPoints(WritingGrade grade, Rubric rubric, BigDecimal passBand) {
        if (passBand == null) return grade;
        Map<String, Integer> order = new HashMap<>();
        for (RubricCriterion criterion : rubric.criteria()) {
            if (criterion != null && criterion.code() != null) order.putIfAbsent(criterion.code(), criterion.sortOrder());
        }
        List<WritingGrade.Criterion> weak = grade.criteria().stream()
                .filter(item -> item.knowledgePointId() != null && item.band().compareTo(passBand) < 0)
                .sorted(Comparator.comparing(WritingGrade.Criterion::band)
                        .thenComparingInt(item -> order.getOrDefault(item.code(), Integer.MAX_VALUE)))
                .toList();
        if (weak.isEmpty()) return grade;
        Map<UUID, LearningContentClient.KnowledgePoint> details = knowledgePointDetails(
                weak.stream().map(WritingGrade.Criterion::knowledgePointId).collect(Collectors.toSet()));
        List<WritingGrade.StudyPoint> points = new ArrayList<>();
        for (WritingGrade.Criterion criterion : weak) {
            LearningContentClient.KnowledgePoint point = details.get(criterion.knowledgePointId());
            String reason = point == null ? null : firstText(point.canDo(), point.description());
            points.add(new WritingGrade.StudyPoint(WritingGrade.StudyPoint.CRITERION, criterion.code(), criterion.band(),
                    criterion.knowledgePointId(), point == null ? null : point.code(), point == null ? null : point.name(),
                    point == null ? null : point.name(), reason == null ? null : truncate(reason, MAX_ITEM_CHARS)));
        }
        points.addAll(grade.studyPoints());
        return new WritingGrade(grade.criteria(), grade.corrections(), grade.summary(), grade.overallBand(), points);
    }

    /** Content's optional details are captured once; an unavailable catalog must not prevent grading. */
    private Map<UUID, LearningContentClient.KnowledgePoint> knowledgePointDetails(Set<UUID> ids) {
        try {
            Map<UUID, LearningContentClient.KnowledgePoint> found = new HashMap<>();
            for (LearningContentClient.Topic topic : content.getTopicSequence()) {
                if (topic.knowledgePoints() == null) continue;
                for (LearningContentClient.KnowledgePoint point : topic.knowledgePoints()) {
                    if (ids.contains(point.id())) found.putIfAbsent(point.id(), point);
                }
            }
            return found;
        } catch (LearningRequestException exception) {
            return Map.of();
        }
    }

    private static String firstText(String first, String second) {
        if (first != null && !first.isBlank()) return first.trim();
        return second == null || second.isBlank() ? null : second.trim();
    }

    private JsonNode readObject(String reply) {
        if (reply == null) return null;
        int start = reply.indexOf('{'), end = reply.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            JsonNode node = json.readTree(reply.substring(start, end + 1));
            return node != null && node.isObject() ? node : null;
        } catch (Exception exception) { return null; }
    }

    private static List<WritingGrade.Correction> corrections(JsonNode node, String essay,
                                                              Map<String, ErrorType> allowedCodes) {
        if (!node.isArray()) return List.of();
        List<WritingGrade.Correction> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (values.size() == MAX_CORRECTIONS) break;
            String excerpt = item.path("excerpt").isTextual() ? item.get("excerpt").asText() : null;
            String suggestion = item.path("suggestion").isTextual() ? item.get("suggestion").asText() : null;
            String code = item.path("errorCode").asText(null);
            if (excerpt == null || excerpt.isBlank() || !essay.contains(excerpt) || suggestion == null
                    || code == null || !allowedCodes.containsKey(code)) continue;
            ErrorType errorType = allowedCodes.get(code);
            values.add(new WritingGrade.Correction(truncate(excerpt, MAX_ITEM_CHARS),
                    truncate(suggestion, MAX_ITEM_CHARS), errorType.category(), code));
        }
        return List.copyOf(values);
    }

    private static String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }

    private static String loadTemplate() {
        try (InputStream input = EssayGrader.class.getResourceAsStream("/essay-prompt-template-v1.txt")) {
            if (input == null) throw new IllegalStateException("Essay prompt template missing");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Essay prompt template unreadable", exception);
        }
    }

    public record GradedEssay(WritingGrade grade, String promptVersion) {}
}
