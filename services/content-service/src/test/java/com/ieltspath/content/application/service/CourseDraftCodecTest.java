package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.domain.exception.InvalidCourseDraftException;
import com.ieltspath.content.domain.exception.InvalidCourseDraftException.Problem;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseDraftCodecTest {

    private final ObjectMapper json = new ObjectMapper();
    private final CourseDraftCodec codec = new CourseDraftCodec(json);
    private final UUID kp = UUID.randomUUID();

    private JsonNode node(String text) throws Exception {
        return json.readTree(text);
    }

    private static String lesson(String code) {
        return "{\"code\":\"" + code + "\",\"title\":\"Lesson\",\"knowledgePointIds\":[],\"blocks\":[]}";
    }

    private static String draft(String course, String topics) {
        return "{\"course\":" + course + ",\"topics\":" + topics + "}";
    }

    private static final String COURSE = "{\"code\":\"C1\",\"name\":\"Course\",\"bandLevel\":6.5}";

    private static String topic(String code, int sortOrder, String lessons) {
        return "{\"code\":\"" + code + "\",\"name\":\"Topic\",\"skill\":\"READING\",\"sortOrder\":" + sortOrder
                + ",\"lessons\":" + lessons + "}";
    }

    private List<Problem> problemsOf(String body) throws Exception {
        return assertThrows(InvalidCourseDraftException.class, () -> codec.parse(node(body))).getIssues();
    }

    @Test
    void aCompleteDraftIsReadIntoItsStructure() throws Exception {
        CourseDraftStructure plan = codec.parse(node(draft(COURSE, "[" + topic("T1", 1, "[" + lesson("L1") + "]")
                + "]").replace("\"blocks\":[]", "\"blocks\":[{\"type\":\"TEXT\",\"text\":\"Hi\"},"
                + "{\"type\":\"EXERCISE\",\"questionRefs\":[\"ref-1\"]}]")));

        assertEquals("C1", plan.course().code());
        assertEquals(1, plan.topics().size());
        assertEquals(2, plan.topics().get(0).lessons().get(0).blocks().size());
        assertEquals(List.of("ref-1"), plan.topics().get(0).lessons().get(0).blocks().get(1).questionRefs());
    }

    @Test
    void anEmptyObjectIsAnEmptyDraft() throws Exception {
        CourseDraftStructure plan = codec.parse(node("{}"));

        assertEquals(null, plan.course());
        assertTrue(plan.topics().isEmpty());
    }

    @Test
    void unknownFieldsAreIgnored() throws Exception {
        assertEquals("C1", codec.parse(node("{\"course\":{\"code\":\"C1\",\"name\":\"N\",\"bandLevel\":5.5,"
                + "\"colour\":\"red\"},\"extra\":1}")).course().code());
    }

    @Test
    void requiredFieldsAreReportedWithTheirPath() throws Exception {
        List<Problem> problems = problemsOf("{\"course\":{\"code\":\"\",\"name\":\"N\"},\"topics\":[{\"code\":\"T\"}]}");

        assertTrue(problems.contains(new Problem("course.code", "REQUIRED")));
        assertTrue(problems.contains(new Problem("course.bandLevel", "REQUIRED")));
        assertTrue(problems.contains(new Problem("topics[0].name", "REQUIRED")));
        assertTrue(problems.contains(new Problem("topics[0].skill", "REQUIRED")));
        assertTrue(problems.contains(new Problem("topics[0].sortOrder", "REQUIRED")));
    }

    @Test
    void codesBandsAndLengthsAreChecked() throws Exception {
        List<Problem> problems = problemsOf(draft(
                "{\"code\":\"has space\",\"name\":\"" + "n".repeat(256) + "\",\"bandLevel\":6.3}", "[]"));

        assertTrue(problems.contains(new Problem("course.code", "INVALID_VALUE")));
        assertTrue(problems.contains(new Problem("course.name", "TOO_LONG")));
        assertTrue(problems.contains(new Problem("course.bandLevel", "INVALID_VALUE")));
    }

    @Test
    void codesAndSortOrdersUsedTwiceAreDuplicates() throws Exception {
        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, "[" + lesson("L1") + "," + lesson("L1")
                + "]") + "," + topic("T1", 1, "[]") + "]"));

        assertTrue(problems.contains(new Problem("topics[0].lessons[1].code", "DUPLICATE")));
        assertTrue(problems.contains(new Problem("topics[1].code", "DUPLICATE")));
        assertTrue(problems.contains(new Problem("topics[1].sortOrder", "DUPLICATE")));
    }

    @Test
    void aFinalTestCodeMayNotRepeatAnother() throws Exception {
        List<Problem> problems = problemsOf("{\"course\":" + COURSE + ",\"topics\":[{\"code\":\"T1\",\"name\":\"N\","
                + "\"skill\":\"READING\",\"sortOrder\":1,\"lessons\":[],\"topicTest\":{\"code\":\"X\",\"title\":\"T\"}}],"
                + "\"courseTest\":{\"code\":\"X\",\"title\":\"T\"}}");

        assertTrue(problems.contains(new Problem("courseTest.code", "DUPLICATE")));
    }

    @Test
    void blockRulesFollowTheirType() throws Exception {
        String badBlocks = "[{\"type\":\"TEXT\",\"questionRefs\":[\"q\"]},{\"type\":\"EXERCISE\"},"
                + "{\"type\":\"EXERCISE\",\"text\":\"no\",\"questionRefs\":[\"q\"]},"
                + "{\"type\":\"EXERCISE\",\"questionRefs\":[\"q\"],\"knowledgePointIds\":[\"" + kp + "\"]},"
                + "{\"type\":\"ASSET\"},{\"text\":\"no type\"}]";
        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, "[{\"code\":\"L1\",\"title\":\"L\","
                + "\"blocks\":" + badBlocks + "}]") + "]"));

        String at = "topics[0].lessons[0].blocks";
        assertTrue(problems.contains(new Problem(at + "[0].questionRefs", "INVALID_VALUE")));
        assertTrue(problems.contains(new Problem(at + "[1].questionRefs", "REQUIRED")));
        assertTrue(problems.contains(new Problem(at + "[2].text", "INVALID_VALUE")));
        assertTrue(problems.contains(new Problem(at + "[3].knowledgePointIds", "INVALID_VALUE")));
        assertTrue(problems.contains(new Problem(at + "[4].type", "INVALID_VALUE")));
        assertTrue(problems.contains(new Problem(at + "[5].type", "REQUIRED")));
    }

    @Test
    void countsAreLimited() throws Exception {
        String lessons = "[" + String.join(",", java.util.Collections.nCopies(51, lesson("L"))) + "]";
        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, lessons) + "]"));

        assertTrue(problems.contains(new Problem("topics[0].lessons", "TOO_MANY")));
    }

    @Test
    void milestonesNeedAPackageAndAReachableTopicPosition() throws Exception {
        List<Problem> problems = problemsOf("{\"course\":" + COURSE + ",\"topics\":[" + topic("T1", 1, "[]") + "],"
                + "\"practiceTestMilestones\":[{\"afterTopicSequence\":1},{\"packageId\":\"" + UUID.randomUUID()
                + "\",\"afterTopicSequence\":2}]}");

        assertTrue(problems.contains(new Problem("practiceTestMilestones[0].packageId", "REQUIRED")));
        assertTrue(problems.contains(new Problem("practiceTestMilestones[1].afterTopicSequence", "INVALID_VALUE")));
    }

    @Test
    void anUnreadableStructureIsRefused() throws Exception {
        assertEquals(List.of(new Problem("$", "INVALID_VALUE")), problemsOf("{\"topics\":\"not a list\"}"));
        assertEquals(List.of(new Problem("$", "INVALID_VALUE")),
                problemsOf("{\"course\":{\"code\":\"C\",\"name\":\"N\",\"bandLevel\":\"high\"}}"));
        assertEquals(List.of(new Problem("$", "REQUIRED")), problemsOf("null"));
    }

    @Test
    void writingAndReadingKeepsTheStructure() throws Exception {
        CourseDraftStructure plan = codec.parse(node(draft(COURSE, "[" + topic("T1", 1, "[" + lesson("L1") + "]")
                + "]")));

        assertEquals(plan, codec.read(codec.write(plan)));
    }
}
