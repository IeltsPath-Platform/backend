package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.result.CourseCandidates;
import com.ieltspath.content.application.result.CourseCandidates.Candidate;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.CourseDraftStructure.BlockPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.CourseInfo;
import com.ieltspath.content.domain.vo.CourseDraftStructure.FinalTestPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.TopicPlan;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns the model's reply into a course structure that only holds what the candidate set allows. A knowledge point,
 * practice set or question the model names that is not a candidate is dropped, as is one used a second time; each drop is
 * recorded as a warning. Codes the model makes up for topics, lessons and tests are cleaned and made unique.
 */
@Component
public class CourseDraftValidator {

    public static final String HALLUCINATED_REFERENCE = "HALLUCINATED_REFERENCE";
    public static final String DUPLICATE_REFERENCE = "DUPLICATE_REFERENCE";
    public static final String EMPTY_LESSON_DROPPED = "EMPTY_LESSON_DROPPED";
    public static final String EMPTY_TOPIC_DROPPED = "EMPTY_TOPIC_DROPPED";

    /** {@code subject} is the offending reference as the model wrote it, or the code of the dropped lesson or topic. */
    public record Warning(String code, String subject) {
    }

    public record Built(CourseDraftStructure structure, List<Warning> warnings) {
    }

    /**
     * @param firstSortOrder sort order for the first topic; the others follow it one by one
     * @return the structure, or {@code null} when nothing usable is left (no lesson teaches any candidate)
     */
    public Built build(CourseDraftRequest request, CourseCandidates candidates, JsonNode reply, int firstSortOrder) {
        Map<String, UUID> pointByCode = new HashMap<>();
        Map<UUID, Candidate> candidateById = new HashMap<>();
        for (Candidate candidate : candidates.points()) {
            pointByCode.put(candidate.code().toLowerCase(Locale.ROOT), candidate.id());
            candidateById.put(candidate.id(), candidate);
        }
        Map<String, UUID> setByCode = new HashMap<>();
        candidates.points().forEach(point -> point.practiceSets()
                .forEach(set -> setByCode.put(set.code().toLowerCase(Locale.ROOT), set.packageId())));
        Map<String, UUID> questionByLabel = new HashMap<>();
        CoursePromptBuilder.questionLabels(candidates)
                .forEach((label, id) -> questionByLabel.put(label.toLowerCase(Locale.ROOT), id));

        List<Warning> warnings = new ArrayList<>();
        Set<UUID> usedPoints = new HashSet<>();
        Set<UUID> usedSets = new HashSet<>();
        Set<UUID> usedQuestions = new HashSet<>();
        Set<String> usedCodes = new HashSet<>();

        List<TopicPlan> topics = new ArrayList<>();
        for (JsonNode topicNode : reply.path("topics")) {
            List<LessonPlan> lessons = new ArrayList<>();
            for (JsonNode lessonNode : topicNode.path("lessons")) {
                List<UUID> points = references(lessonNode.path("knowledgePoints"), pointByCode, usedPoints, warnings);
                if (points.isEmpty()) {
                    warnings.add(new Warning(EMPTY_LESSON_DROPPED, text(lessonNode.path("code"), 100)));
                    continue;
                }
                List<UUID> sets = references(lessonNode.path("practiceSets"), setByCode, usedSets, warnings);
                List<UUID> questions = references(lessonNode.path("questions"), questionByLabel, usedQuestions,
                        warnings);
                List<BlockPlan> blocks = questions.isEmpty() ? List.of() : List.of(new BlockPlan(BlockType.EXERCISE,
                        null, List.of(), questions.stream().map(UUID::toString).toList()));
                lessons.add(new LessonPlan(unique(code(lessonNode.path("code"), "LESSON_" + (lessons.size() + 1)),
                        usedCodes), text(lessonNode.path("title"), 255, "Lesson"), summary(lessonNode.path("summary")),
                        points, blocks, sets, outline(lessonNode.path("summaryMarkdown")), minutes(lessonNode.path("estimatedMinutes")),
                        List.of()));
            }
            if (lessons.isEmpty()) {
                warnings.add(new Warning(EMPTY_TOPIC_DROPPED, text(topicNode.path("code"), 100)));
                continue;
            }
            int number = topics.size() + 1;
            String topicCode = unique(code(topicNode.path("code"), "TOPIC_" + number), usedCodes);
            String name = text(topicNode.path("name"), 255, "Topic " + number);
            topics.add(new TopicPlan(topicCode, name, skill(topicNode.path("skill"), request),
                    firstSortOrder + topics.size(), lessons, finalTest(topicNode.path("topicTest"), topicCode + "_TEST",
                    name + " - final test", usedCodes)));
        }
        if (topics.isEmpty()) {
            return null;
        }
        JsonNode courseNode = reply.path("course");
        String courseCode = code(courseNode.path("code"), "COURSE_" + request.bandLevel().toPlainString()
                .replace('.', '_'));
        CourseInfo course = new CourseInfo(courseCode, text(courseNode.path("name"), 255,
                "IELTS " + request.bandLevel().toPlainString()), request.bandLevel());
        FinalTestPlan courseTest = finalTest(reply.path("courseTest"), courseCode + "_TEST", course.name()
                + " - final test", usedCodes);
        return new Built(new CourseDraftStructure(course, topics, courseTest, List.of(), List.of()), warnings);
    }

    /** Each name in the list is resolved against the candidates; unknown ones and repeats are dropped with a warning. */
    private static List<UUID> references(JsonNode names, Map<String, UUID> candidates, Set<UUID> used,
                                         List<Warning> warnings) {
        List<UUID> resolved = new ArrayList<>();
        for (JsonNode node : names) {
            String name = node.asText("").trim();
            UUID id = candidates.get(name.toLowerCase(Locale.ROOT));
            if (id == null) {
                warnings.add(new Warning(HALLUCINATED_REFERENCE, text(node, 100)));
            } else if (!used.add(id)) {
                warnings.add(new Warning(DUPLICATE_REFERENCE, name));
            } else {
                resolved.add(id);
            }
        }
        return resolved;
    }

    private static FinalTestPlan finalTest(JsonNode node, String defaultCode, String defaultTitle, Set<String> used) {
        return new FinalTestPlan(unique(code(node.path("code"), defaultCode), used),
                text(node.path("title"), 255, defaultTitle));
    }

    private static Skill skill(JsonNode node, CourseDraftRequest request) {
        try {
            Skill named = Skill.valueOf(node.asText("").trim().toUpperCase(Locale.ROOT));
            if (request.skills().contains(named)) return named;
        } catch (IllegalArgumentException unknown) {
            // fall through to the request's first skill
        }
        return request.skills().get(0);
    }

    /** A code the model wrote, cleaned to letters, digits, dots, underscores and dashes; the default when nothing is left. */
    static String code(JsonNode node, String fallback) {
        String cleaned = node.asText("").trim().replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("^_+|_+$", "");
        if (cleaned.length() > 100) cleaned = cleaned.substring(0, 100);
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private static String unique(String code, Set<String> used) {
        String candidate = code;
        int suffix = 2;
        while (!used.add(candidate.toLowerCase(Locale.ROOT))) {
            String tail = "_" + suffix++;
            candidate = (code.length() + tail.length() > 100 ? code.substring(0, 100 - tail.length()) : code) + tail;
        }
        return candidate;
    }

    private static String text(JsonNode node, int max, String fallback) {
        String value = text(node, max);
        return value.isEmpty() ? fallback : value;
    }

    private static String text(JsonNode node, int max) {
        String value = node.asText("").replaceAll("\\s+", " ").trim();
        return value.length() > max ? value.substring(0, max) : value;
    }

    /** A short outline the model wrote from the knowledge points of the lesson; kept within a sensible length. */
    private static String outline(JsonNode node) {
        String value = node.asText("").trim();
        return value.isEmpty() ? null : value.length() > 3000 ? value.substring(0, 3000) : value;
    }

    /** Minutes the model estimated; anything outside 1 to 240 is ignored rather than trusted. */
    private static Integer minutes(JsonNode node) {
        if (!node.isNumber()) return null;
        int value = node.asInt();
        return value >= 1 && value <= 240 ? value : null;
    }

    private static String summary(JsonNode node) {
        String value = node.asText("").trim();
        return value.isEmpty() ? null : value.length() > 2000 ? value.substring(0, 2000) : value;
    }
}
