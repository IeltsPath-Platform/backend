package com.ieltspath.content.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.InvalidCourseDraftException;
import com.ieltspath.content.domain.exception.InvalidCourseDraftException.Problem;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.CourseDraftStructure.BlockPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.FinalTestPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.OptionPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.PracticeSetPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.QuestionPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.TopicPlan;
import com.ieltspath.content.domain.vo.QuestionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Reads and writes the JSON of a course draft and checks its syntax: required fields, lengths, counts and names used
 * twice. It does not look at the catalogue; that is the coverage analysis.
 */
@Component
public class CourseDraftCodec {

    static final int MAX_TOPICS = 30;
    static final int MAX_LESSONS = 50;
    static final int MAX_BLOCKS = 50;
    static final int MAX_QUESTION_REFS = 100;
    static final int MAX_KNOWLEDGE_POINTS = 50;
    static final int MAX_PRACTICE_SETS = 20;
    static final int MAX_RESOURCES = 20;
    static final int MAX_SUMMARY_MARKDOWN = 20000;
    static final int MAX_MINUTES = 600;
    static final int MAX_NEW_WORDS = 20;
    static final int MAX_VOCABULARY = 50;
    static final int MAX_LEMMA = 100;
    static final int MAX_WORD_TEXT = 1000;
    static final int MAX_NEW_PRACTICE_SETS = 3;
    static final int MIN_SET_QUESTIONS = 3;
    static final int MAX_SET_QUESTIONS = 10;
    static final int MAX_SET_PASSAGE = 6000;
    static final int MAX_SET_INSTRUCTIONS = 1000;
    static final int MAX_QUESTION_TEXT = 4000;
    static final int MAX_OPTIONS = 6;
    static final int MAX_OPTION_KEY = 20;
    /** The parts of speech the library knows; a proposed word must name one. */
    static final Set<String> PARTS_OF_SPEECH = Set.of("NOUN", "VERB", "ADJECTIVE", "ADVERB", "PREPOSITION",
            "CONJUNCTION", "IDIOM", "PHRASAL_VERB");
    static final Pattern LEMMA = Pattern.compile("[\\p{L}][\\p{L}\\p{N}' .-]*");
    private static final Pattern WEB_LINK = Pattern.compile("(?i)^https?://\\S+$");
    static final int MAX_MILESTONES = 100;
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final BigDecimal NINE = new BigDecimal("9");

    private final ObjectMapper json;

    public CourseDraftCodec(ObjectMapper json) {
        this.json = json.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /** Reads a structure sent by a client. A body that cannot be read, or that is not valid, is refused. */
    public CourseDraftStructure parse(JsonNode node) {
        CourseDraftStructure structure;
        try {
            structure = json.treeToValue(node, CourseDraftStructure.class);
        } catch (JsonProcessingException | IllegalArgumentException unreadable) {
            throw new InvalidCourseDraftException(List.of(new Problem("$", "INVALID_VALUE")));
        }
        if (structure == null) {
            throw new InvalidCourseDraftException(List.of(new Problem("$", "REQUIRED")));
        }
        List<Problem> problems = problems(structure);
        if (!problems.isEmpty()) {
            throw new InvalidCourseDraftException(problems);
        }
        return structure;
    }

    /** Reads a structure the service stored itself. */
    public CourseDraftStructure read(String stored) {
        try {
            return stored == null ? CourseDraftStructure.empty() : json.readValue(stored, CourseDraftStructure.class);
        } catch (JsonProcessingException unreadable) {
            throw new InvalidContentStateException("Stored course draft is unreadable");
        }
    }

    public String write(CourseDraftStructure structure) {
        try {
            return json.writeValueAsString(structure);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Course draft could not be written", failure);
        }
    }

    /** The syntax problems of a structure; empty when it is well formed. */
    public List<Problem> problems(CourseDraftStructure draft) {
        List<Problem> problems = new ArrayList<>();
        if (draft.course() != null) {
            code(problems, "course.code", draft.course().code());
            text(problems, "course.name", draft.course().name(), 255);
            band(problems, "course.bandLevel", draft.course().bandLevel());
        }
        if (draft.topics().size() > MAX_TOPICS) problems.add(new Problem("topics", "TOO_MANY"));
        Set<String> topicCodes = new HashSet<>();
        Set<String> lessonCodes = new HashSet<>();
        Set<String> packageCodes = new HashSet<>();
        Set<Integer> sortOrders = new HashSet<>();
        for (int t = 0; t < draft.topics().size(); t++) {
            TopicPlan topic = draft.topics().get(t);
            String at = "topics[" + t + "]";
            if (topic == null) {
                problems.add(new Problem(at, "REQUIRED"));
                continue;
            }
            code(problems, at + ".code", topic.code());
            if (topic.code() != null && !topicCodes.add(topic.code())) problems.add(new Problem(at + ".code", "DUPLICATE"));
            text(problems, at + ".name", topic.name(), 255);
            if (topic.skill() == null) problems.add(new Problem(at + ".skill", "REQUIRED"));
            if (topic.sortOrder() == null) {
                problems.add(new Problem(at + ".sortOrder", "REQUIRED"));
            } else if (topic.sortOrder() < 0) {
                problems.add(new Problem(at + ".sortOrder", "INVALID_VALUE"));
            } else if (!sortOrders.add(topic.sortOrder())) {
                problems.add(new Problem(at + ".sortOrder", "DUPLICATE"));
            }
            finalTest(problems, at + ".topicTest", topic.topicTest(), packageCodes);
            if (topic.lessons().size() > MAX_LESSONS) problems.add(new Problem(at + ".lessons", "TOO_MANY"));
            for (int l = 0; l < topic.lessons().size(); l++) {
                lesson(problems, at + ".lessons[" + l + "]", topic.lessons().get(l), lessonCodes);
            }
        }
        finalTest(problems, "courseTest", draft.courseTest(), packageCodes);
        if (draft.practiceTestMilestones().size() > MAX_MILESTONES) {
            problems.add(new Problem("practiceTestMilestones", "TOO_MANY"));
        }
        for (int m = 0; m < draft.practiceTestMilestones().size(); m++) {
            var milestone = draft.practiceTestMilestones().get(m);
            String at = "practiceTestMilestones[" + m + "]";
            if (milestone == null || milestone.packageId() == null) {
                problems.add(new Problem(at + ".packageId", "REQUIRED"));
            } else if (milestone.afterTopicSequence() < 1 || milestone.afterTopicSequence() > Math.max(1,
                    draft.topics().size())) {
                problems.add(new Problem(at + ".afterTopicSequence", "INVALID_VALUE"));
            }
        }
        return problems;
    }

    private static void lesson(List<Problem> problems, String at, LessonPlan lesson, Set<String> lessonCodes) {
        if (lesson == null) {
            problems.add(new Problem(at, "REQUIRED"));
            return;
        }
        code(problems, at + ".code", lesson.code());
        if (lesson.code() != null && !lessonCodes.add(lesson.code())) problems.add(new Problem(at + ".code", "DUPLICATE"));
        text(problems, at + ".title", lesson.title(), 255);
        if (lesson.summary() != null && lesson.summary().length() > 2000) problems.add(new Problem(at + ".summary", "TOO_LONG"));
        if (lesson.knowledgePointIds().size() > MAX_KNOWLEDGE_POINTS) {
            problems.add(new Problem(at + ".knowledgePointIds", "TOO_MANY"));
        }
        if (lesson.summaryMarkdown() != null && lesson.summaryMarkdown().length() > MAX_SUMMARY_MARKDOWN) {
            problems.add(new Problem(at + ".summaryMarkdown", "TOO_LONG"));
        }
        if (lesson.estimatedMinutes() != null && (lesson.estimatedMinutes() < 1 || lesson.estimatedMinutes() > MAX_MINUTES)) {
            problems.add(new Problem(at + ".estimatedMinutes", "OUT_OF_RANGE"));
        }
        if (lesson.resources().size() > MAX_RESOURCES) problems.add(new Problem(at + ".resources", "TOO_MANY"));
        // A lesson has one slide; any number of documents.
        if (lesson.resources().stream().filter(r -> r != null && r.type() == com.ieltspath.content.domain.vo.LessonResourceType.SLIDE).count() > 1) {
            problems.add(new Problem(at + ".resources", "TOO_MANY_SLIDES"));
        }
        for (int r = 0; r < lesson.resources().size(); r++) {
            var resource = lesson.resources().get(r);
            String where = at + ".resources[" + r + "]";
            if (resource == null || resource.type() == null) {
                problems.add(new Problem(where + ".type", "REQUIRED"));
                continue;
            }
            text(problems, where + ".title", resource.title(), 255);
            if (resource.url() == null || resource.url().length() > 2000 || !WEB_LINK.matcher(resource.url().trim()).matches()) {
                problems.add(new Problem(where + ".url", "INVALID"));
            }
        }
        vocabulary(problems, at, lesson);
        if (lesson.practiceSetPackageIds().size() > MAX_PRACTICE_SETS) {
            problems.add(new Problem(at + ".practiceSetPackageIds", "TOO_MANY"));
        }
        newPracticeSets(problems, at, lesson);
        if (lesson.blocks().size() > MAX_BLOCKS) problems.add(new Problem(at + ".blocks", "TOO_MANY"));
        for (int b = 0; b < lesson.blocks().size(); b++) {
            block(problems, at + ".blocks[" + b + "]", lesson.blocks().get(b));
        }
    }

    private static void vocabulary(List<Problem> problems, String at, LessonPlan lesson) {
        if (lesson.newVocabulary().size() > MAX_NEW_WORDS) problems.add(new Problem(at + ".newVocabulary", "TOO_MANY"));
        if (lesson.vocabularySenseIds().size() + lesson.newVocabulary().size() > MAX_VOCABULARY) {
            problems.add(new Problem(at + ".vocabularySenseIds", "TOO_MANY"));
        }
        if (lesson.vocabularySenseIds().stream().anyMatch(java.util.Objects::isNull)) {
            problems.add(new Problem(at + ".vocabularySenseIds", "REQUIRED"));
        }
        for (int w = 0; w < lesson.newVocabulary().size(); w++) {
            var word = lesson.newVocabulary().get(w);
            String where = at + ".newVocabulary[" + w + "]";
            if (word == null) {
                problems.add(new Problem(where, "REQUIRED"));
                continue;
            }
            if (word.lemma() == null || word.lemma().isBlank()) {
                problems.add(new Problem(where + ".lemma", "REQUIRED"));
            } else if (word.lemma().length() > MAX_LEMMA) {
                problems.add(new Problem(where + ".lemma", "TOO_LONG"));
            } else if (!LEMMA.matcher(word.lemma().trim()).matches()) {
                problems.add(new Problem(where + ".lemma", "INVALID_VALUE"));
            }
            if (word.partOfSpeech() == null || !PARTS_OF_SPEECH.contains(word.partOfSpeech())) {
                problems.add(new Problem(where + ".partOfSpeech", "INVALID_VALUE"));
            }
            text(problems, where + ".vietnameseMeaning", word.vietnameseMeaning(), MAX_WORD_TEXT);
            text(problems, where + ".exampleSentence", word.exampleSentence(), MAX_WORD_TEXT);
            if (word.englishDefinition() != null && word.englishDefinition().length() > MAX_WORD_TEXT) {
                problems.add(new Problem(where + ".englishDefinition", "TOO_LONG"));
            }
            if (word.ipa() != null && word.ipa().length() > MAX_LEMMA) problems.add(new Problem(where + ".ipa", "TOO_LONG"));
        }
    }

    /**
     * The sets the model wrote for a lesson. Each is a passage and 3 to 10 choice questions on points the lesson teaches;
     * the answer must be one of the options, and a true/false/not-given question offers exactly those three.
     */
    private static void newPracticeSets(List<Problem> problems, String at, LessonPlan lesson) {
        if (lesson.newPracticeSets().size() > MAX_NEW_PRACTICE_SETS
                || lesson.practiceSetPackageIds().size() + lesson.newPracticeSets().size() > MAX_PRACTICE_SETS) {
            problems.add(new Problem(at + ".newPracticeSets", "TOO_MANY"));
        }
        Set<UUID> taught = new HashSet<>(lesson.allKnowledgePointIds());
        for (int s = 0; s < lesson.newPracticeSets().size(); s++) {
            PracticeSetPlan set = lesson.newPracticeSets().get(s);
            String where = at + ".newPracticeSets[" + s + "]";
            if (set == null) {
                problems.add(new Problem(where, "REQUIRED"));
                continue;
            }
            text(problems, where + ".title", set.title(), 255);
            if (set.instructions() != null && set.instructions().length() > MAX_SET_INSTRUCTIONS) {
                problems.add(new Problem(where + ".instructions", "TOO_LONG"));
            }
            text(problems, where + ".passage", set.passage(), MAX_SET_PASSAGE);
            if (set.questionType() != QuestionType.MULTIPLE_CHOICE
                    && set.questionType() != QuestionType.TRUE_FALSE_NOT_GIVEN) {
                problems.add(new Problem(where + ".questionType", "INVALID_VALUE"));
            }
            if (set.knowledgePointIds().isEmpty() || set.knowledgePointIds().stream().anyMatch(java.util.Objects::isNull)) {
                problems.add(new Problem(where + ".knowledgePointIds", "REQUIRED"));
            } else if (!taught.containsAll(set.knowledgePointIds())) {
                problems.add(new Problem(where + ".knowledgePointIds", "NOT_IN_LESSON"));
            }
            if (set.questions().size() < MIN_SET_QUESTIONS) problems.add(new Problem(where + ".questions", "TOO_FEW"));
            if (set.questions().size() > MAX_SET_QUESTIONS) problems.add(new Problem(where + ".questions", "TOO_MANY"));
            for (int q = 0; q < set.questions().size(); q++) {
                setQuestion(problems, where + ".questions[" + q + "]", set.questions().get(q), set.questionType(),
                        new HashSet<>(set.knowledgePointIds()));
            }
        }
    }

    private static void setQuestion(List<Problem> problems, String at, QuestionPlan question, QuestionType type,
                                    Set<UUID> setPoints) {
        if (question == null) {
            problems.add(new Problem(at, "REQUIRED"));
            return;
        }
        // A question tests points of its own set; none listed means all of them.
        if (question.knowledgePointIds().stream().anyMatch(point -> point == null || !setPoints.contains(point))) {
            problems.add(new Problem(at + ".knowledgePointIds", "NOT_IN_LESSON"));
        }
        text(problems, at + ".stem", question.stem(), MAX_QUESTION_TEXT);
        text(problems, at + ".explanation", question.explanation(), MAX_QUESTION_TEXT);
        if (question.options().size() < 2 || question.options().size() > MAX_OPTIONS) {
            problems.add(new Problem(at + ".options", "INVALID_VALUE"));
            return;
        }
        Set<String> keys = new HashSet<>();
        for (int o = 0; o < question.options().size(); o++) {
            OptionPlan option = question.options().get(o);
            String where = at + ".options[" + o + "]";
            if (option == null || option.optionKey() == null || option.optionKey().isBlank()
                    || option.optionKey().length() > MAX_OPTION_KEY) {
                problems.add(new Problem(where + ".optionKey", "INVALID_VALUE"));
            } else if (!keys.add(option.optionKey())) {
                problems.add(new Problem(where + ".optionKey", "DUPLICATE"));
            }
            text(problems, where + ".content", option == null ? null : option.content(), MAX_QUESTION_TEXT);
        }
        if (question.correctKey() == null || !keys.contains(question.correctKey())) {
            problems.add(new Problem(at + ".correctKey", "NOT_AN_OPTION"));
        }
        if (type == QuestionType.TRUE_FALSE_NOT_GIVEN && !keys.equals(Set.of("TRUE", "FALSE", "NOT_GIVEN"))) {
            problems.add(new Problem(at + ".options", "INVALID_VALUE"));
        }
    }

    private static void block(List<Problem> problems, String at, BlockPlan block) {
        if (block == null || block.type() == null) {
            problems.add(new Problem(at + ".type", "REQUIRED"));
            return;
        }
        if (block.knowledgePointIds().size() > MAX_KNOWLEDGE_POINTS) {
            problems.add(new Problem(at + ".knowledgePointIds", "TOO_MANY"));
        }
        switch (block.type()) {
            case TEXT -> {
                if (block.text() != null && block.text().length() > 20000) problems.add(new Problem(at + ".text", "TOO_LONG"));
                if (!block.questionRefs().isEmpty()) problems.add(new Problem(at + ".questionRefs", "INVALID_VALUE"));
            }
            case EXERCISE -> {
                if (block.text() != null && !block.text().isBlank()) problems.add(new Problem(at + ".text", "INVALID_VALUE"));
                if (!block.knowledgePointIds().isEmpty()) {
                    // An exercise's points come from its questions.
                    problems.add(new Problem(at + ".knowledgePointIds", "INVALID_VALUE"));
                }
                if (block.questionRefs().isEmpty()) {
                    problems.add(new Problem(at + ".questionRefs", "REQUIRED"));
                } else if (block.questionRefs().size() > MAX_QUESTION_REFS) {
                    problems.add(new Problem(at + ".questionRefs", "TOO_MANY"));
                } else if (block.questionRefs().stream().anyMatch(ref -> ref.isBlank() || ref.length() > 100)) {
                    problems.add(new Problem(at + ".questionRefs", "INVALID_VALUE"));
                }
            }
            default -> problems.add(new Problem(at + ".type", "INVALID_VALUE"));
        }
    }

    private static void finalTest(List<Problem> problems, String at, FinalTestPlan test, Set<String> packageCodes) {
        if (test == null) return;
        code(problems, at + ".code", test.code());
        text(problems, at + ".title", test.title(), 255);
        if (test.code() != null && !packageCodes.add(test.code())) problems.add(new Problem(at + ".code", "DUPLICATE"));
    }

    private static void code(List<Problem> problems, String at, String value) {
        if (value == null || value.isBlank()) {
            problems.add(new Problem(at, "REQUIRED"));
        } else if (!CODE.matcher(value).matches()) {
            problems.add(new Problem(at, "INVALID_VALUE"));
        }
    }

    private static void text(List<Problem> problems, String at, String value, int max) {
        if (value == null || value.isBlank()) {
            problems.add(new Problem(at, "REQUIRED"));
        } else if (value.length() > max) {
            problems.add(new Problem(at, "TOO_LONG"));
        }
    }

    private static void band(List<Problem> problems, String at, BigDecimal band) {
        if (band == null) {
            problems.add(new Problem(at, "REQUIRED"));
        } else if (band.signum() < 0 || band.compareTo(NINE) > 0
                || band.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            problems.add(new Problem(at, "INVALID_VALUE"));
        }
    }
}
