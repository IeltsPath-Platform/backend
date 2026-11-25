package com.ieltspath.content.domain.vo;

import com.ieltspath.content.domain.vo.CourseImportModel.Row;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseImportOutlineTest {

    private static Row row(int number, String... pairs) {
        Map<String, String> cells = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) cells.put(pairs[i], pairs[i + 1]);
        return new Row(number, cells);
    }

    @Test
    void groupsLessonsSetsAndTestsUnderTheirTopicEvenWhenValuesAreWrong() {
        Map<String, List<Row>> sheets = new LinkedHashMap<>();
        sheets.put("Course", List.of(row(3, "code", "C1", "name", "Course", "bandLevel", "4.5")));
        sheets.put("Topics", List.of(row(3, "code", "T2", "name", "Second", "skill", "READING", "sortOrder", "20"),
                row(4, "code", "T1", "name", "First", "skill", "NOPE", "sortOrder", "10")));
        sheets.put("Lessons", List.of(row(3, "code", "L2", "topicCode", "T1", "title", "B", "sortOrder", "2"),
                row(4, "code", "L1", "topicCode", "T1", "title", "A", "sortOrder", "1", "knowledgePointCodes", "K1; K2")));
        sheets.put("Blocks", List.of(row(3, "lessonCode", "L1", "type", "EXERCISE", "questionRefs", "Q1, Q2")));
        sheets.put("Questions", List.of(row(3, "importRef", "Q1", "type", "FILL_IN_BLANK", "skill", "READING")));
        sheets.put("PracticeSets", List.of(row(3, "packageCode", "PS1", "title", "Set", "lessonCode", "L1",
                "questionRefs", "Q1")));
        sheets.put("Tests", List.of(row(3, "kind", "TOPIC_TEST", "packageCode", "TT", "topicCode", "T1"),
                row(4, "kind", "COURSE_TEST", "packageCode", "CT"),
                row(5, "kind", "PRACTICE_TEST_MILESTONE", "packageCode", "PM", "afterTopicSequence", "1")));

        var outline = CourseImportOutline.from(new CourseImportModel("1", sheets));

        assertEquals(List.of("T1", "T2"), outline.topics().stream().map(t -> t.code()).toList());
        var first = outline.topics().get(0);
        assertEquals(List.of("L1", "L2"), first.lessons().stream().map(l -> l.code()).toList());
        assertEquals(List.of("K1", "K2"), first.lessons().get(0).knowledgePointCodes());
        assertEquals(List.of("Q1", "Q2"), first.lessons().get(0).blocks().get(0).questionRefs());
        assertEquals("PS1", first.lessons().get(0).practiceSets().get(0).code());
        assertEquals("TT", first.tests().get(0).code());
        assertEquals("CT", outline.courseTests().get(0).code());
        assertEquals("1", outline.milestones().get(0).afterTopicSequence());
        assertEquals(1, outline.questions().size());
    }
}
