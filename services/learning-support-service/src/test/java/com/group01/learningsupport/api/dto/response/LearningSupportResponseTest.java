package com.group01.learningsupport.api.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.group01.learningsupport.application.result.DeckItemResult;
import com.group01.learningsupport.application.result.FlashcardDeckResult;
import com.group01.learningsupport.application.result.StreakResult;
import com.group01.learningsupport.domain.vo.FlashcardSourceType;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningSupportResponseTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void deckAndDeckItemJsonKeepsExistingPropertyNamesAndEnums() {
        var deck = FlashcardDeckResponse.from(new FlashcardDeckResult(UUID.randomUUID(), UUID.randomUUID(),
                "Travel", null, LibraryStatus.ACTIVE, Instant.EPOCH, Instant.EPOCH));
        var deckItem = DeckItemResponse.from(new DeckItemResult(UUID.randomUUID(), UUID.randomUUID(), 4,
                Instant.EPOCH, "front", "back", FlashcardSourceType.MANUAL, LibraryStatus.ACTIVE));

        var deckJson = objectMapper.valueToTree(deck);
        var itemJson = objectMapper.valueToTree(deckItem);

        assertTrue(deckJson.has("description"));
        assertEquals("ACTIVE", deckJson.path("status").asText());
        assertEquals(4, itemJson.path("sortOrder").asInt());
        assertEquals("MANUAL", itemJson.path("sourceType").asText());
        assertEquals("ACTIVE", itemJson.path("status").asText());
    }

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
