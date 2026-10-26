package com.ieltspath.library.api.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ieltspath.library.application.result.DeckItemResult;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalLibraryResponseTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void deckAndDeckItemJsonKeepsExistingPropertyNamesAndEnums() {
        var deck = FlashcardDeckResponse.from(new FlashcardDeckResult(UUID.randomUUID(), UUID.randomUUID(),
                "Travel", null, LibraryStatus.ACTIVE, Instant.EPOCH, Instant.EPOCH,
                "images/u1/deck.png", "https://storage/deck.png", 5L, 2L));
        var deckItem = DeckItemResponse.from(new DeckItemResult(UUID.randomUUID(), UUID.randomUUID(), 4,
                Instant.EPOCH, "front", "back", FlashcardSourceType.MANUAL, LibraryStatus.ACTIVE,
                "https://storage/item.png", "A sample sentence.", 4, "/æp.əl/"));

        var deckJson = objectMapper.valueToTree(deck);
        var itemJson = objectMapper.valueToTree(deckItem);

        assertTrue(deckJson.has("description"));
        assertEquals("ACTIVE", deckJson.path("status").asText());
        assertEquals("images/u1/deck.png", deckJson.path("coverImageKey").asText());
        assertEquals("https://storage/deck.png", deckJson.path("coverImageUrl").asText());
        assertEquals(5, deckJson.path("cardCount").asLong());
        assertEquals(2, deckJson.path("dueCount").asLong());

        assertEquals(4, itemJson.path("sortOrder").asInt());
        assertEquals("MANUAL", itemJson.path("sourceType").asText());
        assertEquals("ACTIVE", itemJson.path("status").asText());
        assertEquals("https://storage/item.png", itemJson.path("imageUrl").asText());
        assertEquals("A sample sentence.", itemJson.path("exampleSentence").asText());
        assertEquals(4, itemJson.path("leitnerBox").asInt());
        assertEquals("/æp.əl/", itemJson.path("ipa").asText());
    }

    @Test
    void flashcardResponseContainsImageAndSentenceFields() {
        var card = FlashcardResponse.from(new FlashcardResult(
                UUID.randomUUID(), UUID.randomUUID(), FlashcardSourceType.MANUAL, null, null, null,
                "apple", "quả táo", LibraryStatus.ACTIVE, Instant.EPOCH, Instant.EPOCH, 1,
                null, null, 0, null, null, "I eat an apple.",
                "images/u1/apple.png", "https://storage/apple.png"
        ));
        var cardJson = objectMapper.valueToTree(card);
        assertEquals("images/u1/apple.png", cardJson.path("imageKey").asText());
        assertEquals("https://storage/apple.png", cardJson.path("imageUrl").asText());
        assertEquals("I eat an apple.", cardJson.path("exampleSentence").asText());
    }
}
