package com.group01.user.api.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.group01.user.application.result.StreakResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningSupportResponseTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void streakJsonRetainsDateAndTimezoneNames() {
        var response = StreakResponse.from(new StreakResult(UUID.randomUUID(), 3, 5,
                LocalDate.parse("2026-09-22"), "Asia/Ho_Chi_Minh"));

        var json = objectMapper.valueToTree(response);

        assertEquals(3, json.path("currentDays").asInt());
        assertEquals("2026-09-22", json.path("lastQualifiedDate").asText());
        assertEquals("Asia/Ho_Chi_Minh", json.path("timezone").asText());
    }
}
