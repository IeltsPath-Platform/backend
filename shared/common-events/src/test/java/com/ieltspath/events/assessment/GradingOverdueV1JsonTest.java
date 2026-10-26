package com.ieltspath.events.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradingOverdueV1JsonTest {
    @Test
    void preservesCamelCaseFieldsAndTimestamps() throws Exception {
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var event = new GradingOverdueV1(id(1), GradingOverdueV1.EVENT_TYPE, Instant.parse("2026-10-23T09:10:00Z"),
                id(2), id(3), id(4), "WRITING", Instant.parse("2026-10-23T09:00:00Z"));
        byte[] body = json.writeValueAsBytes(event);
        assertEquals(json.readTree("""
                {"eventId":"30000000-0000-4000-8000-000000000001","eventType":"assessment.grading-overdue.v1",
                 "occurredAt":"2026-10-23T09:10:00Z","learnerId":"30000000-0000-4000-8000-000000000002",
                 "examinerId":"30000000-0000-4000-8000-000000000003","gradingJobId":"30000000-0000-4000-8000-000000000004",
                 "skill":"WRITING","deadline":"2026-10-23T09:00:00Z"}
                """), json.readTree(body));
        assertEquals(event, json.readValue(body, GradingOverdueV1.class));
    }

    private static UUID id(int suffix) {
        return UUID.fromString("30000000-0000-4000-8000-" + String.format("%012d", suffix));
    }
}
