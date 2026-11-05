package com.ieltspath.events.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradingCompletedV1JsonTest {
    @Test
    void preservesCamelCaseFieldsAndCarriesNoGradeOrEssay() throws Exception {
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var event = new GradingCompletedV1(id(1), GradingCompletedV1.EVENT_TYPE, Instant.parse("2026-10-26T13:00:00Z"),
                id(2), id(3), id(4), "WRITING");
        byte[] body = json.writeValueAsBytes(event);
        assertEquals(json.readTree("""
                {"eventId":"30000000-0000-4000-8000-000000000001","eventType":"assessment.grading-completed.v1",
                 "occurredAt":"2026-10-26T13:00:00Z","learnerId":"30000000-0000-4000-8000-000000000002",
                 "gradingJobId":"30000000-0000-4000-8000-000000000003",
                 "reviewSubmissionId":"30000000-0000-4000-8000-000000000004","skill":"WRITING"}
                """), json.readTree(body));
        assertEquals(event, json.readValue(body, GradingCompletedV1.class));
    }

    private static UUID id(int suffix) {
        return UUID.fromString("30000000-0000-4000-8000-" + String.format("%012d", suffix));
    }
}
