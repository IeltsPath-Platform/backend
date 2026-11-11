package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.ieltspath.learning.domain.service.WritingBandCaps;
import com.ieltspath.learning.domain.service.WritingScore;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Uses Content's active rubric during bootstrap, then the task's activated immutable rubric version. */
@Component
public class EssayGrader {
    static final int MAX_ITEM_CHARS = 300;
    static final int MAX_FOCUS_POINTS = 4;
    static final int MAX_FOCUS_TITLE_CHARS = 80;
    static final int MAX_CORRECTIONS = 10;
    /** Error code of a figure the essay reports wrongly; the catalog decides whether it exists. */
    static final String DATA_MISREPORTED_CODE = "T1_DATA_MISREPORTED";
    static final int MAX_SUMMARY_CHARS = 600;
    static final int MAX_REASON_CHARS = 300;
    static final int MAX_EVIDENCE = 3;
    static final int MAX_PRIORITIES = 3;
    static final int MAX_EXAMPLE_CHARS = 200;
    static final String TEMPLATE_VERSION = "v1";
    static final String TEMPLATE_VERSION_2 = "v2";
    static final String PROMPT_TEMPLATE = loadTemplate(TEMPLATE_VERSION);
    static final String PROMPT_TEMPLATE_2 = loadTemplate(TEMPLATE_VERSION_2);

    private final LlmClient llm;
    private final ObjectMapper json;
    private final LearningContentClient content;
    private final EssayPromptActivationProvider activations;
    private final EssayKnowledgeJudge knowledgeJudge;
    private final String model;
    private final WritingSettings settings;
    private io.micrometer.core.instrument.MeterRegistry metrics;

    @Autowired
    public EssayGrader(LlmClient llm, ObjectMapper json, LearningContentClient content,
                       EssayPromptActivationProvider activations, EssayKnowledgeJudge knowledgeJudge,
                       @Value("${learning.llm.model:}") String model, WritingSettings settings) {
        this.llm = llm;
        this.json = json;
        this.content = content;
        this.activations = activations;
        this.knowledgeJudge = knowledgeJudge;
        this.model = model;
        this.settings = settings;
    }

    public EssayGrader(LlmClient llm, ObjectMapper json, LearningContentClient content,
                       EssayPromptActivationProvider activations, EssayKnowledgeJudge knowledgeJudge) {
        this(llm, json, content, activations, knowledgeJudge, "", WritingSettings.defaults());
    }

    /** Kept for unit tests that construct the original grader directly. */
    public EssayGrader(LlmClient llm, ObjectMapper json, LearningContentClient content,
                       EssayPromptActivationProvider activations) {
        this(llm, json, content, activations, new EssayKnowledgeJudge());
    }

    /** Optional: without a registry the grader still grades, it only stops counting missing evidence. */
    @Autowired(required = false)
    void setMetrics(io.micrometer.core.instrument.MeterRegistry metrics) { this.metrics = metrics; }

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
            errorTypes = content.activeErrorTypes("WRITING");
        } catch (LearningRequestException exception) {
            throw new WritingGradingException("RUBRIC_UNAVAILABLE");
        }
        if (rubric == null || rubric.criteria() == null || rubric.criteria().isEmpty()
                || rubric.code() == null || rubric.version() < 1 || !task.name().equals(rubric.task())
                || errorTypes == null) {
            throw new WritingGradingException("RUBRIC_UNAVAILABLE");
        }
        // The activated version names the template; with nothing activated the first template stays in use.
        String templateVersion = expectedVersion == null ? TEMPLATE_VERSION : templateVersionOf(expectedVersion);
        String promptVersion = rubric.code() + "@" + rubric.version() + "+tpl" + templateVersion;
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
        String system = systemPrompt(task, rubric, errorTypes, templateVersion);
        String user = userPrompt(task, prompt, essay);
        Supplier<Map<UUID, LearningContentClient.KnowledgePoint>> catalog = memoized(this::knowledgePointCatalog);
        JsonNode root;
        try {
            root = readObject(llm.completeJson(system, user));
        } catch (LlmUnavailableException exception) {
            throw new WritingGradingException("LLM_UNAVAILABLE");
        }
        WritingGrade parsed = parse(task, essay, allowedCodes, criterionPoints, root, catalog,
                TEMPLATE_VERSION_2.equals(templateVersion))
                .orElseThrow(() -> new WritingGradingException("INVALID_GRADE"));
        WritingGrade grade = applyBandCaps(parsed, task, essay, prompt.minWords(), settings);
        WritingGrade withCriteria = withCriterionStudyPoints(grade, rubric, prompt.passBand(), catalog);
        WritingGrade judged = knowledgeJudge.judge(task, essay, withCriteria, rubric.criteria(), allowedCodes,
                root.path("judgments"), catalog);
        return new GradedEssay(judged.withModel(model), promptVersion);
    }

    private Rubric resolveRubric(WritingTask task, String expectedVersion) {
        if (expectedVersion == null) return content.getActiveRubric(task.name());
        String[] parts = expectedVersion.split("@|\\+tpl", -1);
        if (parts.length != 3 || parts[0].isBlank() || !isKnownTemplate(parts[2])) {
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

    private static boolean isKnownTemplate(String version) {
        return TEMPLATE_VERSION.equals(version) || TEMPLATE_VERSION_2.equals(version);
    }

    private static String templateVersionOf(String activatedVersion) {
        String[] parts = activatedVersion.split("@|\\+tpl", -1);
        if (parts.length != 3 || !isKnownTemplate(parts[2])) throw new WritingGradingException("PROMPT_VERSION_UNAVAILABLE");
        return parts[2];
    }

    static String systemPrompt(WritingTask task, Rubric rubric, List<ErrorType> errorTypes) {
        return systemPrompt(task, rubric, errorTypes, TEMPLATE_VERSION);
    }

    static String systemPrompt(WritingTask task, Rubric rubric, List<ErrorType> errorTypes, String templateVersion) {
        String criterionText = rubric.criteria().stream().sorted(Comparator.comparingInt(RubricCriterion::sortOrder))
                .map(EssayGrader::renderCriterion).collect(Collectors.joining("\n\n"));
        String errorCodes = errorTypes.stream().filter(Objects::nonNull).filter(error -> error.code() != null)
                .sorted(Comparator.comparing(ErrorType::code))
                .map(error -> error.code() + " — " + error.name() + ": " + error.fixHint())
                .collect(Collectors.joining("\n"));
        String taskName = task == WritingTask.TASK_1 ? "Academic Writing Task 1" : "Writing Task 2";
        String template = forTask(TEMPLATE_VERSION_2.equals(templateVersion) ? PROMPT_TEMPLATE_2 : PROMPT_TEMPLATE,
                task == WritingTask.TASK_1);
        return template.replace("{{task}}", taskName)
                .replace("{{criteria}}", criterionText)
                .replace("{{error_codes}}", errorCodes.isBlank() ? "(none)" : errorCodes)
                + EssayKnowledgeJudge.prompt(task, rubric.criteria());
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

    static String neutralize(String value, String tag) {
        return value.replaceAll("(?i)</\\s*" + tag + "\\s*>", "[/" + tag + "]");
    }

    private Optional<WritingGrade> parse(WritingTask task, String essay, Map<String, ErrorType> allowedCodes,
                                         Map<String, UUID> criterionPoints, JsonNode root,
                                         Supplier<Map<UUID, LearningContentClient.KnowledgePoint>> catalog,
                                         boolean secondTemplate) {
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
            List<String> evidence = secondTemplate ? quotations(node.path("evidence"), essay) : List.of();
            if (secondTemplate && evidence.isEmpty() && metrics != null) {
                metrics.counter("learning.writing.evidence_missing").increment();
            }
            String descriptor = secondTemplate && node.path("descriptor").isTextual()
                    && !node.get("descriptor").asText().isBlank()
                    ? truncate(node.get("descriptor").asText().trim(), MAX_ITEM_CHARS) : null;
            byCode.put(code, new WritingGrade.Criterion(code, value.setScale(1), List.of(),
                    comment.isBlank() ? List.of() : List.of(comment), criterionPoints.get(code), List.of(), evidence,
                    descriptor));
        }
        if (byCode.size() != task.criteria().size()) return Optional.empty();
        List<WritingGrade.Criterion> criteria = task.criteria().stream().map(byCode::get).toList();
        String summary = truncate(root.path("summary").asText(""), MAX_SUMMARY_CHARS);
        WritingGrade.Task1Checks task1Checks = null;
        WritingGrade.Task2Checks task2Checks = null;
        JsonNode correctionNodes = root.path("corrections");
        if (secondTemplate) {
            if (task == WritingTask.TASK_1) {
                task1Checks = TaskChecksParser.task1(root.path("taskChecks"), essay);
                correctionNodes = withDataCorrections(correctionNodes, task1Checks, essay);
            } else {
                task2Checks = TaskChecksParser.task2(root.path("taskChecks"), essay);
            }
        }
        List<WritingGrade.Correction> corrections = corrections(correctionNodes, essay, allowedCodes, catalog,
                secondTemplate);
        WritingGrade grade = new WritingGrade(criteria, corrections,
                summary, WritingScore.overallBand(criteria.stream().map(WritingGrade.Criterion::band).toList()),
                focusPoints(root.path("focus")));
        if (!secondTemplate) return Optional.of(grade);
        Set<String> codes = corrections.stream().map(WritingGrade.Correction::errorCode).collect(Collectors.toSet());
        return Optional.of(grade.withDetails(priorities(root.path("priorities"), essay, codes),
                errorExamples(root.path("errorExamples"), codes)).withChecks(task1Checks, task2Checks));
    }

    /**
     * The model's corrections plus one for every figure it judged WRONG that none of them covers, so a wrong figure
     * always reaches the learner as a correction of its own kind. A figure is covered only by a correction of that
     * kind whose quotation overlaps the sentence: a grammar correction on the same words does not say the figure is
     * wrong. Nothing is added without a statement of what the chart shows.
     */
    private JsonNode withDataCorrections(JsonNode modelCorrections, WritingGrade.Task1Checks checks, String essay) {
        ArrayNode merged = json.createArrayNode();
        if (modelCorrections.isArray()) merged.addAll((ArrayNode) modelCorrections);
        for (WritingGrade.DataCheck check : checks.dataChecks()) {
            if (!WritingGrade.DataCheck.WRONG.equals(check.verdict()) || check.expected() == null
                    || covered(merged, check.excerpt(), essay)) continue;
            ObjectNode item = json.createObjectNode();
            item.put("excerpt", check.excerpt());
            item.put("suggestion", check.expected());
            item.put("errorCode", DATA_MISREPORTED_CODE);
            item.put("certainty", WritingGrade.Correction.ERROR);
            merged.add(item);
        }
        return merged;
    }

    private static boolean covered(ArrayNode corrections, String sentence, String essay) {
        int sentenceStart = essay.indexOf(sentence);
        if (sentenceStart < 0) return false;
        int sentenceEnd = sentenceStart + sentence.length();
        for (JsonNode item : corrections) {
            if (!DATA_MISREPORTED_CODE.equals(item.path("errorCode").asText(null))) continue;
            String excerpt = item.path("excerpt").isTextual() ? item.get("excerpt").asText() : "";
            int start = excerpt.isBlank() ? -1 : essay.indexOf(excerpt);
            if (start >= 0 && start < sentenceEnd && sentenceStart < start + excerpt.length()) return true;
        }
        return false;
    }

    /** Quotations the model gave that are not exact parts of the essay are dropped; at most three are kept. */
    private static List<String> quotations(JsonNode node, String essay) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (values.size() == MAX_EVIDENCE) break;
            if (!item.isTextual() || item.asText().isBlank() || !essay.contains(item.asText())) continue;
            values.add(truncate(item.asText(), MAX_ITEM_CHARS));
        }
        return List.copyOf(values);
    }

    /** At most three; a quotation not in the essay or a code not among the corrections is dropped, and so is an empty one. */
    private static List<WritingGrade.Priority> priorities(JsonNode node, String essay, Set<String> correctionCodes) {
        if (!node.isArray()) return List.of();
        List<WritingGrade.Priority> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (values.size() == MAX_PRIORITIES) break;
            List<String> excerpts = quotations(item.path("excerpts"), essay);
            String title = item.path("title").isTextual() ? item.get("title").asText().trim() : "";
            if (excerpts.isEmpty() || title.isBlank()) continue;
            List<String> codes = new ArrayList<>();
            if (item.path("errorCodes").isArray()) {
                for (JsonNode code : item.get("errorCodes")) {
                    if (code.isTextual() && correctionCodes.contains(code.asText()) && !codes.contains(code.asText())) {
                        codes.add(code.asText());
                    }
                }
            }
            values.add(new WritingGrade.Priority(truncate(title, MAX_FOCUS_TITLE_CHARS),
                    truncate(item.path("explanation").asText(""), MAX_ITEM_CHARS), excerpts,
                    truncate(item.path("action").asText(""), MAX_ITEM_CHARS), codes));
        }
        return List.copyOf(values);
    }

    /** One example per error code that appears in the corrections; the first one wins. */
    private static List<WritingGrade.ErrorExample> errorExamples(JsonNode node, Set<String> correctionCodes) {
        if (!node.isArray()) return List.of();
        Map<String, WritingGrade.ErrorExample> byCode = new LinkedHashMap<>();
        for (JsonNode item : node) {
            String code = item.path("errorCode").asText(null);
            String wrong = item.path("wrong").isTextual() ? item.get("wrong").asText().trim() : "";
            String right = item.path("right").isTextual() ? item.get("right").asText().trim() : "";
            if (code == null || !correctionCodes.contains(code) || byCode.containsKey(code) || wrong.isBlank()
                    || right.isBlank()) continue;
            byCode.put(code, new WritingGrade.ErrorExample(code, truncate(wrong, MAX_EXAMPLE_CHARS),
                    truncate(right, MAX_EXAMPLE_CHARS)));
        }
        return List.copyOf(byCode.values());
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
    /**
     * Applies the centre's band caps when they are on; with them off the grade is returned unchanged. The length rule
     * needs only the essay; the overview, partial-answer and position rules read the checks of the second template.
     * The off-topic rule has no check behind it yet, so it never fires.
     */
    static WritingGrade applyBandCaps(WritingGrade grade, WritingTask task, String essay, Integer minWords,
                                      WritingSettings settings) {
        if (!settings.bandCapsEnabled() || minWords == null) return grade;
        WritingBandCaps.Caps caps = new WritingBandCaps.Caps(settings.tooShortBandCap(), settings.offTopicBandCap(),
                settings.noOverviewBandCap(), settings.partialAnswerBandCap(), settings.noPositionBandCap());
        WritingBandCaps.Flags flags = new WritingBandCaps.Flags(false,
                grade.task1Checks() != null && Boolean.FALSE.equals(grade.task1Checks().hasOverview()),
                grade.task2Checks() != null && grade.task2Checks().partialAnswer(),
                grade.task2Checks() != null && grade.task2Checks().noPosition());
        List<WritingBandCaps.Limit> limits = WritingBandCaps.evaluate(task, WritingScore.countWords(essay), minWords,
                flags, caps);
        if (limits.isEmpty()) return grade;
        Map<String, BigDecimal> before = new LinkedHashMap<>();
        for (WritingGrade.Criterion criterion : grade.criteria()) before.putIfAbsent(criterion.code(), criterion.band());
        List<WritingGrade.AppliedBandLimit> applied = limits.stream()
                .filter(limit -> before.containsKey(limit.criterion()) && before.get(limit.criterion()).compareTo(limit.cap()) > 0)
                .map(limit -> new WritingGrade.AppliedBandLimit(limit.reason().name(), limit.criterion(), limit.cap(),
                        before.get(limit.criterion()), limit.message()))
                .toList();
        if (applied.isEmpty()) return grade;
        Map<String, BigDecimal> after = WritingBandCaps.apply(before, limits);
        List<WritingGrade.Criterion> criteria = grade.criteria().stream()
                .map(criterion -> criterion.withBand(after.get(criterion.code()))).toList();
        BigDecimal overall = WritingScore.overallBand(criteria.stream().map(WritingGrade.Criterion::band).toList());
        return new WritingGrade(criteria, grade.corrections(), grade.summary(), overall, grade.studyPoints(),
                grade.judgmentVersion(), grade.model(), applied, grade.priorities(), grade.errorExamples(),
                grade.task1Checks(), grade.task2Checks());
    }

    private WritingGrade withCriterionStudyPoints(WritingGrade grade, Rubric rubric, BigDecimal passBand,
                                                  Supplier<Map<UUID, LearningContentClient.KnowledgePoint>> catalog) {
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
        Map<UUID, LearningContentClient.KnowledgePoint> details = catalog.get();
        List<WritingGrade.StudyPoint> points = new ArrayList<>();
        for (WritingGrade.Criterion criterion : weak) {
            LearningContentClient.KnowledgePoint point = details.get(criterion.knowledgePointId());
            String reason = point == null ? null : firstText(point.canDo(), point.description());
            points.add(new WritingGrade.StudyPoint(WritingGrade.StudyPoint.CRITERION, criterion.code(), criterion.band(),
                    criterion.knowledgePointId(), point == null ? null : point.code(), point == null ? null : point.name(),
                    point == null ? null : point.name(), reason == null ? null : truncate(reason, MAX_ITEM_CHARS)));
        }
        points.addAll(grade.studyPoints());
        return new WritingGrade(grade.criteria(), grade.corrections(), grade.summary(), grade.overallBand(), points,
                null, null, grade.bandLimits(), grade.priorities(), grade.errorExamples(), grade.task1Checks(),
                grade.task2Checks());
    }

    /** Content's optional details are captured once; an unavailable catalog must not prevent grading. */
    private Map<UUID, LearningContentClient.KnowledgePoint> knowledgePointCatalog() {
        try {
            Map<UUID, LearningContentClient.KnowledgePoint> found = new HashMap<>();
            for (LearningContentClient.Topic topic : content.getTopicSequence()) {
                if (topic.knowledgePoints() == null) continue;
                for (LearningContentClient.KnowledgePoint point : topic.knowledgePoints()) {
                    if (point != null && point.id() != null) found.putIfAbsent(point.id(), point);
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
        return readObject(json, reply);
    }

    static JsonNode readObject(ObjectMapper json, String reply) {
        if (reply == null) return null;
        int start = reply.indexOf('{'), end = reply.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            JsonNode node = json.readTree(reply.substring(start, end + 1));
            return node != null && node.isObject() ? node : null;
        } catch (Exception exception) { return null; }
    }

    private static <T> Supplier<T> memoized(Supplier<T> source) {
        return new Supplier<>() {
            private boolean loaded;
            private T value;

            @Override
            public synchronized T get() {
                if (!loaded) {
                    value = source.get();
                    loaded = true;
                }
                return value;
            }
        };
    }

    private List<WritingGrade.Correction> corrections(JsonNode node, String essay,
                                                     Map<String, ErrorType> allowedCodes,
                                                     Supplier<Map<UUID, LearningContentClient.KnowledgePoint>> catalog,
                                                     boolean secondTemplate) {
        if (!node.isArray()) return List.of();
        int ceiling = secondTemplate ? settings.maxCorrections() : MAX_CORRECTIONS;
        List<WritingGrade.Correction> values = new ArrayList<>();
        Map<UUID, LearningContentClient.KnowledgePoint> knowledgePoints = null;
        for (JsonNode item : node) {
            if (values.size() == ceiling) break;
            String excerpt = item.path("excerpt").isTextual() ? item.get("excerpt").asText() : null;
            String suggestion = item.path("suggestion").isTextual() ? item.get("suggestion").asText() : null;
            String code = item.path("errorCode").asText(null);
            if (excerpt == null || excerpt.isBlank() || !essay.contains(excerpt) || suggestion == null
                    || code == null || !allowedCodes.containsKey(code)) continue;
            ErrorType errorType = allowedCodes.get(code);
            UUID practiceKnowledgePointId = errorType.knowledgePointId();
            String practiceKnowledgePointCode = null;
            if (practiceKnowledgePointId != null) {
                if (knowledgePoints == null) knowledgePoints = catalog.get();
                LearningContentClient.KnowledgePoint knowledgePoint = knowledgePoints.get(practiceKnowledgePointId);
                if (knowledgePoint == null || !knowledgePoint.hasPracticeSet()) {
                    practiceKnowledgePointId = null;
                } else {
                    practiceKnowledgePointCode = knowledgePoint.code();
                }
            }
            String reason = null;
            String certainty = null;
            if (secondTemplate) {
                // A missing reason does not spoil the correction; a missing or unknown certainty reads as an error.
                String text = item.path("reason").isTextual() ? item.get("reason").asText().trim() : "";
                reason = text.isBlank() ? null : truncate(text, MAX_REASON_CHARS);
                certainty = WritingGrade.Correction.SUGGESTION.equals(item.path("certainty").asText(""))
                        ? WritingGrade.Correction.SUGGESTION : WritingGrade.Correction.ERROR;
            }
            values.add(new WritingGrade.Correction(truncate(excerpt, MAX_ITEM_CHARS),
                    truncate(suggestion, MAX_ITEM_CHARS), errorType.category(), code, errorType.name(),
                    errorType.fixHint(), errorType.severity(), practiceKnowledgePointId, practiceKnowledgePointCode,
                    reason, certainty));
        }
        return List.copyOf(values);
    }

    private static String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }

    private static String loadTemplate(String version) {
        try (InputStream input = EssayGrader.class.getResourceAsStream("/essay-prompt-template-" + version + ".txt")) {
            if (input == null) throw new IllegalStateException("Essay prompt template missing");
            return withoutNotes(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Essay prompt template unreadable", exception);
        }
    }

    /** Lines that start with "#" at the top of a template are notes for people; the model never sees them. */
    private static String withoutNotes(String template) {
        return template.replaceFirst("\\A(?:#[^\\r\\n]*\\R)+\\s*", "");
    }

    /** The template with only the section of the given task kept; a template without sections is unchanged. */
    static String forTask(String template, boolean taskOne) {
        String drop = taskOne ? "TASK_2" : "TASK_1";
        String keep = taskOne ? "TASK_1" : "TASK_2";
        return template.replaceAll("(?s)\\[\\[" + drop + "\\]\\].*?\\[\\[/" + drop + "\\]\\]\\R*", "")
                .replaceAll("\\[\\[/?" + keep + "\\]\\]\\R?", "");
    }

    public record GradedEssay(WritingGrade grade, String promptVersion) {}
}
