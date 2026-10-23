package com.ieltspath.content.domain.service;

import com.ieltspath.content.domain.service.CourseImportValidator.Validated;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportModel.Issue;
import com.ieltspath.content.domain.vo.CourseImportModel.Row;
import com.ieltspath.content.domain.vo.CourseImportPlan;
import com.ieltspath.content.domain.vo.CourseImportTemplate;
import com.ieltspath.content.domain.vo.ImportCatalogFacts;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseImportValidatorTest {

    private final UUID kp1 = UUID.randomUUID();
    private final UUID kp2 = UUID.randomUUID();
    private final ImportCatalogFacts facts = new ImportCatalogFacts(Map.of("KP1", kp1, "KP2", kp2),
            Set.of("ERR_PARA"), Set.of("EXISTING-Q"), Set.of("TAKEN-PKG"));

    /** A small workbook that passes; each test changes one thing. */
    private static final class Sheets {
        final Map<String, List<Row>> rows = new LinkedHashMap<>();
        private final Map<String, Integer> next = new HashMap<>();

        Sheets() {
            CourseImportTemplate.SHEETS.keySet().forEach(sheet -> rows.put(sheet, new ArrayList<>()));
        }

        Sheets add(String sheet, String... pairs) {
            Map<String, String> cells = new HashMap<>();
            CourseImportTemplate.columnNames(sheet).forEach(column -> cells.put(column, ""));
            for (int i = 0; i < pairs.length; i += 2) cells.put(pairs[i], pairs[i + 1]);
            int number = next.merge(sheet, 1, Integer::sum) + 2;
            rows.get(sheet).add(new Row(number, cells));
            return this;
        }

        /** Empties a sheet and starts its row numbers again at 3. */
        void clear(String sheet) {
            rows.get(sheet).clear();
            next.remove(sheet);
        }

        CourseImportModel model() {
            return new CourseImportModel("1", rows);
        }
    }

    private Sheets good() {
        return new Sheets()
                .add("Course", "code", "C1", "name", "Course", "bandLevel", "6.5")
                .add("Topics", "code", "T1", "name", "Topic", "skill", "READING", "sortOrder", "970")
                .add("Topics", "code", "T2", "name", "Topic two", "skill", "READING", "sortOrder", "971")
                .add("Lessons", "code", "L1", "topicCode", "T1", "title", "Lesson one", "sortOrder", "1",
                        "knowledgePointCodes", "KP1")
                .add("Lessons", "code", "L2", "topicCode", "T1", "title", "Lesson two", "sortOrder", "2",
                        "knowledgePointCodes", "KP2")
                .add("Blocks", "lessonCode", "L1", "sortOrder", "1", "type", "TEXT", "text", "Intro",
                        "knowledgePointCodes", "KP1")
                .add("Blocks", "lessonCode", "L1", "sortOrder", "2", "type", "EXERCISE", "questionRefs", "Q1; Q2")
                .add("Questions", "importRef", "Q1", "type", "MULTIPLE_CHOICE", "skill", "READING", "stem", "Stem 1",
                        "optionA", "a", "optionB", "b", "optionC", "c", "answer", "b", "explanation", "why",
                        "difficulty", "MEDIUM", "knowledgePointCodes", "KP1", "distractorErrorCodes", "A=ERR_PARA")
                .add("Questions", "importRef", "Q2", "type", "FILL_IN_BLANK", "skill", "READING", "stem", "Stem 2",
                        "answer", "colour | color", "explanation", "why", "difficulty", "EASY",
                        "knowledgePointCodes", "KP1,KP2")
                .add("PracticeSets", "packageCode", "PS1", "title", "Set 1", "lessonCode", "L1", "questionRefs", "Q1,Q2")
                .add("Tests", "kind", "TOPIC_TEST", "packageCode", "TT1", "title", "Topic test", "topicCode", "T1",
                        "questionRefs", "Q1")
                .add("Tests", "kind", "COURSE_TEST", "packageCode", "CT1", "title", "Course test", "questionRefs", "Q2")
                .add("Tests", "kind", "PRACTICE_TEST_MILESTONE", "packageCode", "PT1", "title", "Checkpoint",
                        "afterTopicSequence", "1", "questionRefs", "EXISTING-Q");
    }

    private Validated check(Sheets sheets) {
        return CourseImportValidator.validate(sheets.model(), facts);
    }

    private static List<String> where(Validated result) {
        return result.errors().stream().map(i -> i.sheet() + ":" + i.row() + ":" + i.column() + ":" + i.code()).toList();
    }

    @Test
    void aGoodWorkbookBecomesAPlanThatCountsWhatItWillCreate() {
        Validated result = check(good());

        assertEquals(List.of(), result.errors());
        assertEquals(new CourseImportValidator.Summary(2, 2, 2, 4), result.summary());
        CourseImportPlan plan = result.plan();
        assertEquals("C1", plan.structure().course().code());
        assertEquals(2, plan.structure().topics().size());
        var firstLesson = plan.structure().topics().get(0).lessons().get(0);
        assertEquals("L1", firstLesson.code());
        assertEquals(List.of(kp1), firstLesson.knowledgePointIds());
        assertEquals(BlockType.TEXT, firstLesson.blocks().get(0).type());
        assertEquals(List.of("Q1", "Q2"), firstLesson.blocks().get(1).questionRefs());
        assertEquals(2, plan.questions().size());
        assertEquals(List.of(PackageType.PRACTICE_SET, PackageType.TOPIC_TEST, PackageType.COURSE_TEST,
                PackageType.PRACTICE_TEST), plan.packages().stream().map(p -> p.type()).toList());
    }

    @Test
    void questionsAreResolvedIntoOptionsAnswersAndDistractors() {
        var q1 = check(good()).plan().questions().get(0);
        var q2 = check(good()).plan().questions().get(1);

        assertEquals(QuestionType.MULTIPLE_CHOICE, q1.type());
        assertEquals("B", q1.correct());
        assertEquals(List.of("A", "B", "C"), q1.options().stream().map(o -> o.key()).toList());
        assertEquals(Map.of("A", "ERR_PARA"), q1.distractors());
        assertEquals(Skill.READING, q1.skill());
        assertEquals(QuestionDifficulty.MEDIUM, q1.difficulty());
        assertEquals(List.of(kp1), q1.knowledgePointIds());
        assertEquals(List.of("colour", "color"), q2.accepted());
        assertEquals(List.of(kp1, kp2), q2.knowledgePointIds());
        assertTrue(q1.choice());
    }

    @Test
    void lessonsAndBlocksFollowTheirSortOrderThenTheirRow() {
        Sheets sheets = good();
        sheets.rows.get("Lessons").set(0, new Row(3, merge(sheets.rows.get("Lessons").get(0), "sortOrder", "9")));
        sheets.rows.get("Blocks").set(0, new Row(3, merge(sheets.rows.get("Blocks").get(0), "sortOrder", "5")));

        var lessons = check(sheets).plan().structure().topics().get(0).lessons();

        assertEquals(List.of("L2", "L1"), lessons.stream().map(l -> l.code()).toList());
        var blocks = lessons.get(1).blocks();
        assertEquals(List.of(BlockType.EXERCISE, BlockType.TEXT), blocks.stream().map(b -> b.type()).toList());
    }

    @Test
    void codesUsedTwiceAreDuplicates() {
        Sheets sheets = good()
                .add("Topics", "code", "T1", "name", "again", "skill", "READING", "sortOrder", "970")
                .add("Lessons", "code", "L1", "topicCode", "T1", "title", "again", "knowledgePointCodes", "KP1")
                .add("Questions", "importRef", "Q1", "type", "FILL_IN_BLANK", "skill", "READING", "stem", "s",
                        "answer", "x", "explanation", "e", "difficulty", "EASY", "knowledgePointCodes", "KP1")
                .add("Tests", "kind", "TOPIC_TEST", "packageCode", "TT1", "title", "t", "topicCode", "T1",
                        "questionRefs", "Q1");

        List<String> errors = where(check(sheets));

        assertTrue(errors.contains("Topics:5:code:DUPLICATE"), errors.toString());
        assertTrue(errors.contains("Topics:5:sortOrder:DUPLICATE"), errors.toString());
        assertTrue(errors.contains("Lessons:5:code:DUPLICATE"), errors.toString());
        assertTrue(errors.contains("Questions:5:importRef:DUPLICATE"), errors.toString());
        assertTrue(errors.contains("Tests:6:packageCode:DUPLICATE"), errors.toString());
    }

    @Test
    void codesThatAlreadyExistAreConflicts() {
        Sheets taken = good();
        taken.clear("Tests");
        taken.add("Tests", "kind", "COURSE_TEST", "packageCode", "TAKEN-PKG", "title", "t", "questionRefs", "Q1");
        taken.clear("Questions");
        taken.add("Questions", "importRef", "EXISTING-Q", "type", "FILL_IN_BLANK", "skill", "READING", "stem", "s",
                "answer", "x", "explanation", "e", "difficulty", "EASY", "knowledgePointCodes", "KP1");

        List<String> errors = where(check(taken));

        assertTrue(errors.contains("Tests:3:packageCode:CODE_CONFLICT"), errors.toString());
        assertTrue(errors.contains("Questions:3:importRef:CODE_CONFLICT"), errors.toString());
    }

    @Test
    void referencesToNothingAreUnknown() {
        Sheets sheets = good();
        sheets.rows.get("Lessons").set(0, new Row(3, merge(sheets.rows.get("Lessons").get(0), "topicCode", "NOPE",
                "knowledgePointCodes", "KP1, GHOST")));
        sheets.rows.get("Blocks").set(1, new Row(4, merge(sheets.rows.get("Blocks").get(1), "lessonCode", "L-NONE")));
        sheets.rows.get("Blocks").set(0, new Row(3, merge(sheets.rows.get("Blocks").get(0), "type", "EXERCISE",
                "questionRefs", "Q1, NOWHERE", "text", "", "knowledgePointCodes", "")));

        List<String> errors = where(check(sheets));

        assertTrue(errors.contains("Lessons:3:topicCode:UNKNOWN_REFERENCE"), errors.toString());
        assertTrue(errors.contains("Lessons:3:knowledgePointCodes:UNKNOWN_KNOWLEDGE_POINT"), errors.toString());
        assertTrue(errors.contains("Blocks:4:lessonCode:UNKNOWN_REFERENCE"), errors.toString());
        assertTrue(errors.contains("Blocks:3:questionRefs:UNKNOWN_QUESTION_REF"), errors.toString());
    }

    @Test
    void aQuestionRefMayNameAnExistingQuestion() {
        assertEquals(List.of(), check(good()).errors(), "PT1 lists EXISTING-Q, which is not in the file");
    }

    @Test
    void aBadQuestionIsReportedCellByCell() {
        Sheets sheets = good();
        sheets.clear("Questions");
        sheets.add("Questions", "importRef", "QA", "type", "MULTIPLE_CHOICE", "skill", "READING", "stem", "s",
                "optionA", "a", "optionB", "b", "answer", "D", "explanation", "e", "difficulty", "EASY",
                "knowledgePointCodes", "KP1");
        sheets.add("Questions", "importRef", "QB", "type", "MULTIPLE_CHOICE", "skill", "READING", "stem", "s",
                "optionA", "only", "answer", "A", "explanation", "e", "difficulty", "EASY", "knowledgePointCodes", "KP1");
        sheets.add("Questions", "importRef", "QC", "type", "FILL_IN_BLANK", "skill", "READING", "stem", "s",
                "answer", " | ", "explanation", "", "difficulty", "", "knowledgePointCodes", "");
        sheets.add("Questions", "importRef", "QD", "type", "ESSAY", "skill", "WRITING", "stem", "s", "answer", "x",
                "explanation", "e", "difficulty", "EASY", "knowledgePointCodes", "KP1");
        sheets.add("Questions", "importRef", "QE", "type", "MULTIPLE_CHOICE", "skill", "READING", "stem", "s",
                "optionA", "a", "optionB", "b", "answer", "A", "explanation", "e", "difficulty", "EASY",
                "knowledgePointCodes", "KP1", "distractorErrorCodes", "A=ERR_PARA; B=NO_SUCH; C=ERR_PARA");

        List<String> errors = where(check(sheets));

        assertTrue(errors.contains("Questions:3:answer:ANSWER_NOT_IN_OPTIONS"), errors.toString());
        assertTrue(errors.contains("Questions:4:optionA:NOT_ENOUGH_OPTIONS"), errors.toString());
        assertTrue(errors.contains("Questions:5:answer:NO_ACCEPTED_ANSWER"), errors.toString());
        assertTrue(errors.contains("Questions:5:explanation:NO_EXPLANATION"), errors.toString());
        assertTrue(errors.contains("Questions:5:difficulty:DIFFICULTY_REQUIRED"), errors.toString());
        assertTrue(errors.contains("Questions:5:knowledgePointCodes:REQUIRED"), errors.toString());
        assertTrue(errors.contains("Questions:6:type:UNSUPPORTED_TYPE"), errors.toString());
        // The right answer cannot also be a distractor; an unknown error code and a missing option are refused.
        assertTrue(errors.contains("Questions:7:distractorErrorCodes:INVALID_VALUE"), errors.toString());
        assertTrue(errors.contains("Questions:7:distractorErrorCodes:UNKNOWN_ERROR_CODE"), errors.toString());
    }

    @Test
    void testsNeedTheirPlace() {
        Sheets sheets = good();
        sheets.clear("Tests");
        sheets.add("Tests", "kind", "TOPIC_TEST", "packageCode", "T-A", "title", "t", "questionRefs", "Q1");
        sheets.add("Tests", "kind", "TOPIC_TEST", "packageCode", "T-B", "title", "t", "topicCode", "T-NONE",
                "questionRefs", "Q1");
        sheets.add("Tests", "kind", "COURSE_TEST", "packageCode", "T-C", "title", "t", "topicCode", "T1",
                "questionRefs", "Q1");
        sheets.add("Tests", "kind", "COURSE_TEST", "packageCode", "T-D", "title", "t", "questionRefs", "Q1");
        sheets.add("Tests", "kind", "COURSE_TEST", "packageCode", "T-E", "title", "t", "questionRefs", "Q1");
        sheets.add("Tests", "kind", "PRACTICE_TEST_MILESTONE", "packageCode", "T-F", "title", "t",
                "afterTopicSequence", "9", "questionRefs", "Q1");
        sheets.add("Tests", "kind", "PRACTICE_TEST_MILESTONE", "packageCode", "T-G", "title", "t", "questionRefs", "Q1");
        sheets.add("Tests", "kind", "MOCK", "packageCode", "T-H", "title", "t", "questionRefs", "Q1");

        List<String> errors = where(check(sheets));

        assertTrue(errors.contains("Tests:3:topicCode:REQUIRED"), errors.toString());
        assertTrue(errors.contains("Tests:4:topicCode:UNKNOWN_REFERENCE"), errors.toString());
        assertTrue(errors.contains("Tests:5:topicCode:INVALID_VALUE"), errors.toString());
        assertTrue(errors.contains("Tests:7:kind:DUPLICATE"), errors.toString());
        assertTrue(errors.contains("Tests:8:afterTopicSequence:INVALID_VALUE"), errors.toString());
        assertTrue(errors.contains("Tests:9:afterTopicSequence:REQUIRED"), errors.toString());
        assertTrue(errors.contains("Tests:10:kind:INVALID_VALUE"), errors.toString());
    }

    @Test
    void aPracticeSetNeedsItsLessonAndQuestions() {
        Sheets sheets = good();
        sheets.clear("PracticeSets");
        sheets.add("PracticeSets", "packageCode", "PS-A", "title", "t", "lessonCode", "L-NONE", "questionRefs", "Q1");
        sheets.add("PracticeSets", "packageCode", "PS-B", "title", "t", "lessonCode", "L1");

        List<String> errors = where(check(sheets));

        assertTrue(errors.contains("PracticeSets:3:lessonCode:UNKNOWN_REFERENCE"), errors.toString());
        assertTrue(errors.contains("PracticeSets:4:questionRefs:REQUIRED"), errors.toString());
    }

    @Test
    void theCourseSheetHoldsOneValidRow() {
        Sheets none = good();
        none.clear("Course");
        assertTrue(where(check(none)).contains("Course:0::REQUIRED"));

        Sheets two = good().add("Course", "code", "C2", "name", "x", "bandLevel", "5");
        assertTrue(where(check(two)).contains("Course:4::DUPLICATE"));

        Sheets bad = good();
        bad.rows.get("Course").set(0, new Row(3, Map.of("code", "bad code", "name", "", "bandLevel", "6.3")));
        List<String> errors = where(check(bad));
        assertTrue(errors.contains("Course:3:code:INVALID_VALUE"), errors.toString());
        assertTrue(errors.contains("Course:3:name:REQUIRED"), errors.toString());
        assertTrue(errors.contains("Course:3:bandLevel:INVALID_VALUE"), errors.toString());
    }

    @Test
    void anExerciseBlockHasNoTextAndATextBlockNoQuestions() {
        Sheets sheets = good();
        sheets.clear("Blocks");
        sheets.add("Blocks", "lessonCode", "L1", "type", "TEXT", "text", "t", "questionRefs", "Q1");
        sheets.add("Blocks", "lessonCode", "L1", "type", "EXERCISE", "text", "no", "questionRefs", "Q1",
                "knowledgePointCodes", "KP1");
        sheets.add("Blocks", "lessonCode", "L1", "type", "EXERCISE");
        sheets.add("Blocks", "lessonCode", "L1", "type", "VIDEO");

        List<String> errors = where(check(sheets));

        assertTrue(errors.contains("Blocks:3:questionRefs:INVALID_VALUE"), errors.toString());
        assertTrue(errors.contains("Blocks:4:text:INVALID_VALUE"), errors.toString());
        assertTrue(errors.contains("Blocks:4:knowledgePointCodes:INVALID_VALUE"), errors.toString());
        assertTrue(errors.contains("Blocks:5:questionRefs:REQUIRED"), errors.toString());
        assertTrue(errors.contains("Blocks:6:type:INVALID_VALUE"), errors.toString());
    }

    @Test
    void aWorkbookWithErrorsHasNoPlanButStillCountsWhatItCanSee() {
        Validated result = check(good().add("Topics", "code", "T1", "name", "dup", "skill", "READING",
                "sortOrder", "1"));

        assertNull(result.plan());
        assertNotNull(result.summary());
        assertTrue(result.summary().topics() >= 2);
    }

    @Test
    void issuesNeverQuoteACell() {
        Sheets sheets = good();
        sheets.rows.get("Topics").set(0, new Row(3, Map.of("code", "SECRET CODE", "name", "n", "skill", "READING",
                "sortOrder", "1")));

        for (Issue issue : check(sheets).errors()) {
            assertTrue(!issue.message().contains("SECRET"), issue.message());
        }
    }

    private static Map<String, String> merge(Row row, String... pairs) {
        Map<String, String> cells = new HashMap<>(row.cells());
        for (int i = 0; i < pairs.length; i += 2) cells.put(pairs[i], pairs[i + 1]);
        return cells;
    }
}
