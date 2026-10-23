package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.port.CourseCandidateReader.PracticeSetRef;
import com.ieltspath.content.application.result.CourseCandidates;
import com.ieltspath.content.application.result.CourseCandidates.Candidate;
import com.ieltspath.content.application.service.CourseDraftValidator.Built;
import com.ieltspath.content.application.service.CourseDraftValidator.Warning;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseDraftValidatorTest {

    private final ObjectMapper json = new ObjectMapper();
    private final CourseDraftValidator validator = new CourseDraftValidator();

    private final UUID paraphrase = UUID.randomUUID();
    private final UUID skimming = UUID.randomUUID();
    private final UUID setOne = UUID.randomUUID();
    private final UUID setTwo = UUID.randomUUID();
    private final UUID question1 = UUID.randomUUID();
    private final UUID question2 = UUID.randomUUID();

    private final CourseCandidates candidates = new CourseCandidates(List.of(
            new Candidate(paraphrase, "R_PARA", "Paraphrase", "d", false,
                    List.of(new PracticeSetRef(setOne, "PS-1")), List.of(question1)),
            new Candidate(skimming, "R_SKIM", "Skimming", "d", false,
                    List.of(new PracticeSetRef(setTwo, "PS-2")), List.of(question2))), List.of());

    private final CourseDraftRequest request = new CourseDraftRequest(new BigDecimal("6.5"),
            List.of(Skill.READING), List.of("paraphrase"), List.of(), 4, null);

    private JsonNode reply(String text) throws Exception {
        return json.readTree(text);
    }

    private static final String GOOD = """
            {"course":{"code":"R65","name":"Reading 6.5"},
             "topics":[{"code":"T-PARA","name":"Paraphrase","skill":"READING",
               "lessons":[
                 {"code":"L1","title":"Paraphrase","summary":"s","knowledgePoints":["R_PARA"],
                  "practiceSets":["PS-1"],"questions":["Q1"]},
                 {"code":"L2","title":"Skim","knowledgePoints":["r_skim"],"practiceSets":["PS-2"],"questions":["Q2"]}],
               "topicTest":{"code":"T-PARA-TEST","title":"Test"}}],
             "courseTest":{"code":"R65-TEST","title":"Final"}}
            """;

    @Test
    void aCleanReplyBecomesAStructureWithRealIds() throws Exception {
        Built built = validator.build(request, candidates, reply(GOOD), 971);

        assertEquals(List.of(), built.warnings());
        var structure = built.structure();
        assertEquals("R65", structure.course().code());
        assertEquals(new BigDecimal("6.5"), structure.course().bandLevel());
        assertEquals(1, structure.topics().size());
        assertEquals(971, structure.topics().get(0).sortOrder());
        assertEquals(Skill.READING, structure.topics().get(0).skill());
        LessonPlan first = structure.topics().get(0).lessons().get(0);
        assertEquals(List.of(paraphrase), first.knowledgePointIds());
        assertEquals(List.of(setOne), first.practiceSetPackageIds());
        assertEquals(BlockType.EXERCISE, first.blocks().get(0).type());
        assertEquals(List.of(question1.toString()), first.blocks().get(0).questionRefs());
        assertEquals(List.of(skimming), structure.topics().get(0).lessons().get(1).knowledgePointIds());
        assertEquals("T-PARA-TEST", structure.topics().get(0).topicTest().code());
        assertEquals("R65-TEST", structure.courseTest().code());
    }

    @Test
    void aReferenceOutsideTheCandidateSetIsDroppedAndReported() throws Exception {
        String reply = GOOD.replace("\"practiceSets\":[\"PS-1\"]", "\"practiceSets\":[\"PS-1\",\"PS-INVENTED\"]")
                .replace("\"questions\":[\"Q1\"]", "\"questions\":[\"Q1\",\"Q99\"]")
                .replace("[\"R_PARA\"]", "[\"R_PARA\",\"R_MADE_UP\"]");

        Built built = validator.build(request, candidates, reply(reply), 1);

        assertEquals(List.of(new Warning("HALLUCINATED_REFERENCE", "R_MADE_UP"),
                new Warning("HALLUCINATED_REFERENCE", "PS-INVENTED"),
                new Warning("HALLUCINATED_REFERENCE", "Q99")), built.warnings());
        LessonPlan first = built.structure().topics().get(0).lessons().get(0);
        assertEquals(List.of(paraphrase), first.knowledgePointIds());
        assertEquals(List.of(setOne), first.practiceSetPackageIds());
    }

    @Test
    void aReferenceUsedTwiceStaysInTheFirstPlace() throws Exception {
        String reply = GOOD.replace("\"knowledgePoints\":[\"r_skim\"],\"practiceSets\":[\"PS-2\"],\"questions\":[\"Q2\"]",
                "\"knowledgePoints\":[\"R_PARA\",\"r_skim\"],\"practiceSets\":[\"PS-1\",\"PS-2\"],"
                        + "\"questions\":[\"Q1\",\"Q2\"]");

        Built built = validator.build(request, candidates, reply(reply), 1);

        assertEquals(3, built.warnings().stream().filter(w -> w.code().equals("DUPLICATE_REFERENCE")).count());
        LessonPlan second = built.structure().topics().get(0).lessons().get(1);
        assertEquals(List.of(skimming), second.knowledgePointIds());
        assertEquals(List.of(setTwo), second.practiceSetPackageIds());
        assertEquals(List.of(question2.toString()), second.blocks().get(0).questionRefs());
    }

    @Test
    void aLessonLeftWithNoKnowledgePointIsDroppedAndSoIsAnEmptyTopic() throws Exception {
        String reply = """
                {"topics":[
                  {"code":"T1","name":"A","skill":"READING","lessons":[
                    {"code":"L-GHOST","title":"x","knowledgePoints":["NOPE"]}]},
                  {"code":"T2","name":"B","skill":"READING","lessons":[
                    {"code":"L-REAL","title":"y","knowledgePoints":["R_PARA"]}]}]}
                """;

        Built built = validator.build(request, candidates, reply(reply), 5);

        assertEquals(List.of(new Warning("HALLUCINATED_REFERENCE", "NOPE"),
                new Warning("EMPTY_LESSON_DROPPED", "L-GHOST"), new Warning("EMPTY_TOPIC_DROPPED", "T1")),
                built.warnings());
        assertEquals(1, built.structure().topics().size());
        assertEquals("T2", built.structure().topics().get(0).code());
        assertEquals(5, built.structure().topics().get(0).sortOrder());
    }

    @Test
    void nothingUsableMeansNoStructure() throws Exception {
        assertNull(validator.build(request, candidates, reply("{\"topics\":[]}"), 1));
        assertNull(validator.build(request, candidates, reply("{}"), 1));
        assertNull(validator.build(request, candidates, reply(
                "{\"topics\":[{\"code\":\"T\",\"lessons\":[{\"code\":\"L\",\"knowledgePoints\":[\"X\"]}]}]}"), 1));
    }

    @Test
    void codesTheModelMakesUpAreCleanedAndMadeUnique() throws Exception {
        String reply = """
                {"course":{"code":"my course!","name":"N"},
                 "topics":[{"code":"T 1","name":"A","skill":"READING","lessons":[
                    {"code":"L 1","title":"a","knowledgePoints":["R_PARA"]},
                    {"code":"L 1","title":"b","knowledgePoints":["R_SKIM"]}]}]}
                """;

        Built built = validator.build(request, candidates, reply(reply), 1);

        assertEquals("my_course", built.structure().course().code());
        assertEquals("T_1", built.structure().topics().get(0).code());
        assertEquals("L_1", built.structure().topics().get(0).lessons().get(0).code());
        assertEquals("L_1_2", built.structure().topics().get(0).lessons().get(1).code());
    }

    @Test
    void missingCodesAndTitlesGetDefaults() throws Exception {
        String reply = """
                {"topics":[{"lessons":[{"knowledgePoints":["R_PARA"]}]}]}
                """;

        Built built = validator.build(request, candidates, reply(reply), 1);

        assertEquals("COURSE_6_5", built.structure().course().code());
        assertEquals("TOPIC_1", built.structure().topics().get(0).code());
        assertEquals("Lesson", built.structure().topics().get(0).lessons().get(0).title());
        assertEquals("TOPIC_1_TEST", built.structure().topics().get(0).topicTest().code());
        assertEquals("COURSE_6_5_TEST", built.structure().courseTest().code());
    }

    @Test
    void aSkillOutsideTheRequestFallsBackToTheRequestedOne() throws Exception {
        String reply = """
                {"topics":[{"code":"T","name":"A","skill":"SPEAKING","lessons":[
                    {"code":"L","title":"a","knowledgePoints":["R_PARA"]}]}]}
                """;

        assertEquals(Skill.READING, validator.build(request, candidates, reply(reply), 1)
                .structure().topics().get(0).skill());
    }

    @Test
    void topicsGetConsecutiveSortOrdersAfterTheExistingOnes() throws Exception {
        String reply = """
                {"topics":[
                  {"code":"A","name":"A","skill":"READING","lessons":[{"code":"L1","title":"a","knowledgePoints":["R_PARA"]}]},
                  {"code":"B","name":"B","skill":"READING","lessons":[{"code":"L2","title":"b","knowledgePoints":["R_SKIM"]}]}]}
                """;

        var topics = validator.build(request, candidates, reply(reply), 4010).structure().topics();

        assertEquals(4010, topics.get(0).sortOrder());
        assertEquals(4011, topics.get(1).sortOrder());
    }

    @Test
    void longTextIsCutToTheLimits() throws Exception {
        String reply = "{\"topics\":[{\"code\":\"T\",\"name\":\"" + "n".repeat(300) + "\",\"skill\":\"READING\","
                + "\"lessons\":[{\"code\":\"" + "c".repeat(150) + "\",\"title\":\"t\",\"summary\":\""
                + "s".repeat(2500) + "\",\"knowledgePoints\":[\"R_PARA\"]}]}]}";

        var lesson = validator.build(request, candidates, reply(reply), 1).structure().topics().get(0).lessons().get(0);

        assertEquals(100, lesson.code().length());
        assertEquals(2000, lesson.summary().length());
        assertTrue(validator.build(request, candidates, reply(reply), 1).structure().topics().get(0).name().length() <= 255);
    }
}
