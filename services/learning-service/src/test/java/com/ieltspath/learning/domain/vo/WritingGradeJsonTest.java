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
}
