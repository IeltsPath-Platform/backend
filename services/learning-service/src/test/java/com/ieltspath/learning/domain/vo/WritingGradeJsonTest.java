package com.ieltspath.learning.domain.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Stored grades are JSON; ones written before criteria carried a knowledge point must still read. */
class WritingGradeJsonTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void aCriterionStoredWithoutAKnowledgePointReadsAsNull() throws Exception {
        var stored = "{\"code\":\"TR\",\"band\":6.5,\"strengths\":[],\"improvements\":[\"Add examples\"]}";

        var criterion = json.readValue(stored, WritingGrade.Criterion.class);

        assertEquals("TR", criterion.code());
        assertEquals(new BigDecimal("6.5"), criterion.band());
        assertNull(criterion.knowledgePointId());
    }

    @Test
    void aCriterionKeepsItsKnowledgePointThroughStorage() throws Exception {
        var point = UUID.randomUUID();
        var criterion = new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of("Link ideas"), point);

        var restored = json.readValue(json.writeValueAsString(criterion), WritingGrade.Criterion.class);

        assertEquals(criterion, restored);
        assertEquals(point, restored.knowledgePointId());
    }

    @Test
    void studyPointsSurviveStorage() throws Exception {
        var points = List.of(new WritingGrade.StudyPoint("CRITERION", "CC", new BigDecimal("5.5"), UUID.randomUUID(),
                "DEMO_WRITING_CRITERION_CC", "Mạch lạc và liên kết (CC)", "Mạch lạc và liên kết (CC)", "Nối câu"),
                new WritingGrade.StudyPoint("AI", null, null, null, null, null, "Linking", "Use linkers"));
        var grade = new WritingGrade(List.of(), List.of(), "Summary", new BigDecimal("6.0"), points);
        assertEquals(grade, json.readValue(json.writeValueAsString(grade), WritingGrade.class));
    }

    @Test
    void aGradeStoredWithoutStudyPointsReadsAsEmpty() throws Exception {
        var grade = json.readValue("{\"criteria\":[],\"corrections\":[],\"summary\":\"x\",\"overallBand\":6.0}", WritingGrade.class);
        assertEquals(List.of(), grade.studyPoints());
    }

    @Test
    void criterionJudgmentsSurviveStorage() throws Exception {
        var point = UUID.randomUUID();
        var criterion = new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), null,
                List.of(new WritingGrade.KnowledgeJudgment(point, "LINKING", "Linking",
                        WritingGrade.Judgment.FAIL, "use a linker", "reason")));

        assertEquals(criterion, json.readValue(json.writeValueAsString(criterion), WritingGrade.Criterion.class));
    }

    @Test
    void aCriterionStoredWithoutJudgmentsReadsAsEmpty() throws Exception {
        var stored = "{\"code\":\"CC\",\"band\":6.0,\"strengths\":[],\"improvements\":[]}";
        var criterion = json.readValue(stored, WritingGrade.Criterion.class);
        assertEquals(List.of(), criterion.knowledgePointJudgments());
    }

    @Test
    void knowledgePointStudyPointSurvivesStorage() throws Exception {
        var point = new WritingGrade.StudyPoint(WritingGrade.StudyPoint.KNOWLEDGE_POINT, "CC",
                new BigDecimal("5.5"), UUID.randomUUID(), "LINKING", "Linking", "Linking", "reason", "excerpt",
                UUID.randomUUID());
        assertEquals(point, json.readValue(json.writeValueAsString(point), WritingGrade.StudyPoint.class));
    }

    @Test
    void aGradeStoredWithoutJudgmentVersionReadsAsNull() throws Exception {
        var grade = json.readValue("{\"criteria\":[],\"corrections\":[],\"summary\":\"x\",\"overallBand\":6.0}",
                WritingGrade.class);
        assertNull(grade.judgmentVersion());
    }
}
