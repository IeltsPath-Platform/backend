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

    @Test
    void aLessonKeepsItsMarkdownSummaryMinutesAndResources() throws Exception {
        String lesson = "{\"code\":\"L1\",\"title\":\"Lesson\",\"summaryMarkdown\":\"- one\\n- two\",\"estimatedMinutes\":15,"
                + "\"resources\":[{\"type\":\"SLIDE\",\"title\":\"Deck\",\"url\":\"https://slides.example/deck\"},"
                + "{\"type\":\"DOCUMENT\",\"title\":\"Sheet\",\"url\":\"https://docs.example/sheet\"}]}";

        var read = codec.parse(node(draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]"))).topics().get(0).lessons().get(0);

        assertEquals("- one\n- two", read.summaryMarkdown());
        assertEquals(15, read.estimatedMinutes());
        assertEquals(2, read.resources().size());
        assertEquals("https://slides.example/deck", read.resources().get(0).url());
    }

    @Test
    void aDraftWithoutTheNewLessonFieldsStillReads() throws Exception {
        var read = codec.parse(node(draft(COURSE, "[" + topic("T1", 1, "[" + lesson("L1") + "]") + "]"))).topics().get(0).lessons().get(0);

        assertEquals(null, read.summaryMarkdown());
        assertEquals(null, read.estimatedMinutes());
        assertEquals(List.of(), read.resources());
    }

    @Test
    void badLessonMinutesAndResourceLinksAreReportedWithTheirPath() throws Exception {
        String lesson = "{\"code\":\"L1\",\"title\":\"Lesson\",\"estimatedMinutes\":0,"
                + "\"resources\":[{\"type\":\"SLIDE\",\"title\":\"Deck\",\"url\":\"ftp://nope\"},"
                + "{\"type\":\"DOCUMENT\",\"title\":\" \",\"url\":\"https://ok.example\"}]}";

        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]"));

        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".lessons[0].estimatedMinutes") && p.problem().equals("OUT_OF_RANGE")));
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".resources[0].url") && p.problem().equals("INVALID")));
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".resources[1].title")));
    }

    @Test
    void aLessonMayHaveOnlyOneSlide() throws Exception {
        String lesson = "{\"code\":\"L1\",\"title\":\"Lesson\",\"resources\":["
                + "{\"type\":\"SLIDE\",\"title\":\"A\",\"url\":\"https://slides.example/a\"},"
                + "{\"type\":\"SLIDE\",\"title\":\"B\",\"url\":\"https://slides.example/b\"},"
                + "{\"type\":\"DOCUMENT\",\"title\":\"D1\",\"url\":\"https://docs.example/1\"},"
                + "{\"type\":\"DOCUMENT\",\"title\":\"D2\",\"url\":\"https://docs.example/2\"}]}";

        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]"));

        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".resources") && p.problem().equals("TOO_MANY_SLIDES")));
    }

    private static String word(String lemma, String partOfSpeech, String meaning) {
        return "{\"lemma\":\"" + lemma + "\",\"partOfSpeech\":\"" + partOfSpeech + "\",\"vietnameseMeaning\":\""
                + meaning + "\",\"exampleSentence\":\"Example.\"}";
    }

    private static String lessonWith(String fields) {
        return "{\"code\":\"L1\",\"title\":\"Lesson\"," + fields + "}";
    }

    @Test
    void aLessonKeepsItsPickedSensesAndProposedWords() throws Exception {
        UUID sense = UUID.randomUUID();
        String lesson = lessonWith("\"vocabularySenseIds\":[\"" + sense + "\"],\"newVocabulary\":[" + word("trend", "NOUN", "xu huong") + "]");

        var read = codec.parse(node(draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]"))).topics().get(0).lessons().get(0);

        assertEquals(List.of(sense), read.vocabularySenseIds());
        assertEquals("trend", read.newVocabulary().get(0).lemma());
        assertEquals(read, codec.read(codec.write(codec.parse(node(draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]")))))
                .topics().get(0).lessons().get(0));
    }

    @Test
    void aDraftWithoutVocabularyReadsAsEmpty() throws Exception {
        var read = codec.parse(node(draft(COURSE, "[" + topic("T1", 1, "[" + lesson("L1") + "]") + "]"))).topics().get(0).lessons().get(0);

        assertEquals(List.of(), read.vocabularySenseIds());
        assertEquals(List.of(), read.newVocabulary());
    }

    @Test
    void badProposedWordsAreReportedWithTheirPath() throws Exception {
        String lesson = lessonWith("\"newVocabulary\":[" + word("", "NOUN", "x") + "," + word("ok", "PRONOUN", "x") + ","
                + word("fine", "NOUN", " ") + "," + word("12 bad!", "NOUN", "x") + "]");

        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]"));

        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".newVocabulary[0].lemma") && p.problem().equals("REQUIRED")));
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".newVocabulary[1].partOfSpeech") && p.problem().equals("INVALID_VALUE")));
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".newVocabulary[2].vietnameseMeaning") && p.problem().equals("REQUIRED")));
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".newVocabulary[3].lemma") && p.problem().equals("INVALID_VALUE")));
    }

    @Test
    void vocabularyIsLimitedPerLesson() throws Exception {
        String words = java.util.stream.IntStream.range(0, 21).mapToObj(n -> word("word", "NOUN", "x"))
                .collect(java.util.stream.Collectors.joining(","));

        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, "[" + lessonWith("\"newVocabulary\":[" + words + "]") + "]") + "]"));

        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".newVocabulary") && p.problem().equals("TOO_MANY")));
    }

    private static String question(String stem, String correct) {
        return "{\"stem\":\"" + stem + "\",\"explanation\":\"Because.\",\"correctKey\":\"" + correct + "\",\"difficulty\":\"MEDIUM\","
                + "\"options\":[{\"optionKey\":\"A\",\"content\":\"x\"},{\"optionKey\":\"B\",\"content\":\"y\"}]}";
    }

    private String set(String type, String points, String... questions) {
        return "{\"title\":\"Set\",\"instructions\":\"Read.\",\"passage\":\"A. Text.\",\"questionType\":\"" + type
                + "\",\"knowledgePointIds\":" + points + ",\"questions\":[" + String.join(",", questions) + "]}";
    }

    private String lessonWithSets(String... sets) {
        return lessonWith("\"knowledgePointIds\":[\"" + kp + "\"],\"newPracticeSets\":[" + String.join(",", sets) + "]");
    }

    @Test
    void aLessonKeepsThePracticeSetsTheModelWroteForIt() throws Exception {
        String lesson = lessonWithSets(set("MULTIPLE_CHOICE", "[\"" + kp + "\"]", question("Q1", "A"), question("Q2", "B"), question("Q3", "A")));
        String body = draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]");

        var read = codec.parse(node(body)).topics().get(0).lessons().get(0);

        assertEquals(1, read.newPracticeSets().size());
        assertEquals("Set", read.newPracticeSets().get(0).title());
        assertEquals(3, read.newPracticeSets().get(0).questions().size());
        assertEquals("A", read.newPracticeSets().get(0).questions().get(0).correctKey());
        assertEquals(read, codec.read(codec.write(codec.parse(node(body)))).topics().get(0).lessons().get(0));
    }

    @Test
    void aDraftWithoutPracticeSetsReadsAsEmpty() throws Exception {
        var read = codec.parse(node(draft(COURSE, "[" + topic("T1", 1, "[" + lesson("L1") + "]") + "]"))).topics().get(0).lessons().get(0);

        assertEquals(List.of(), read.newPracticeSets());
    }

    @Test
    void aBadPracticeSetIsReportedWithItsPath() throws Exception {
        String bad = set("SHORT_ANSWER", "[\"" + UUID.randomUUID() + "\"]", question("Q1", "Z"), question("", "A"));
        String lesson = lessonWithSets(bad);

        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]"));

        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".newPracticeSets[0].questionType") && p.problem().equals("INVALID_VALUE")));
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".newPracticeSets[0].knowledgePointIds") && p.problem().equals("NOT_IN_LESSON")));
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".newPracticeSets[0].questions") && p.problem().equals("TOO_FEW")));
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".questions[0].correctKey") && p.problem().equals("NOT_AN_OPTION")));
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".questions[1].stem") && p.problem().equals("REQUIRED")));
    }

    @Test
    void aQuestionMayTestOnlyPointsOfItsOwnSet() throws Exception {
        String own = question("Q1", "A").replaceFirst("\\{", "{\"knowledgePointIds\":[\"" + kp + "\"],");
        String foreign = question("Q2", "A").replaceFirst("\\{", "{\"knowledgePointIds\":[\"" + UUID.randomUUID() + "\"],");
        String valid = draft(COURSE, "[" + topic("T1", 1, "[" + lessonWithSets(
                set("MULTIPLE_CHOICE", "[\"" + kp + "\"]", own, question("Q2", "A"), question("Q3", "A"))) + "]") + "]");
        String invalid = draft(COURSE, "[" + topic("T1", 1, "[" + lessonWithSets(
                set("MULTIPLE_CHOICE", "[\"" + kp + "\"]", own, foreign, question("Q3", "A"))) + "]") + "]");

        var questions = codec.parse(node(valid)).topics().get(0).lessons().get(0).newPracticeSets().get(0).questions();
        List<Problem> problems = problemsOf(invalid);

        assertEquals(List.of(kp), questions.get(0).knowledgePointIds());
        assertEquals(List.of(), questions.get(1).knowledgePointIds());
        assertEquals(1, problems.stream().filter(p -> p.path().endsWith(".knowledgePointIds")
                && p.path().contains(".questions[") && p.problem().equals("NOT_IN_LESSON")).count());
        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".questions[1].knowledgePointIds")));
    }

    @Test
    void aTrueFalseNotGivenQuestionMustOfferExactlyThoseThreeAnswers() throws Exception {
        String lesson = lessonWithSets(set("TRUE_FALSE_NOT_GIVEN", "[\"" + kp + "\"]", question("Q1", "A"), question("Q2", "A"), question("Q3", "A")));

        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]"));

        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".questions[0].options") && p.problem().equals("INVALID_VALUE")));
    }

    @Test
    void practiceSetsAreLimitedPerLesson() throws Exception {
        String one = set("MULTIPLE_CHOICE", "[\"" + kp + "\"]", question("Q1", "A"), question("Q2", "A"), question("Q3", "A"));
        String lesson = lessonWithSets(one, one, one, one);

        List<Problem> problems = problemsOf(draft(COURSE, "[" + topic("T1", 1, "[" + lesson + "]") + "]"));

        assertTrue(problems.stream().anyMatch(p -> p.path().endsWith(".newPracticeSets") && p.problem().equals("TOO_MANY")));
    }
}
