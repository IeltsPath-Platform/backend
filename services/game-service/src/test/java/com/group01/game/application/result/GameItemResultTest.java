package com.group01.game.application.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GameItemResultTest {
    @Test
    void publicProjectionRetainsVisibleFieldsAndNeverIncludesAnswerMaterial() throws Exception {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("canonicalId", "00000000-0000-0000-0000-000000000001");
        snapshot.put("vocabularySenseId", null);
        snapshot.put("questionVersionId", "00000000-0000-0000-0000-000000000002");
        snapshot.put("prompt", "Choose the answer");
        snapshot.put("options", List.of(Map.of("id", "a", "text", "A")));
        snapshot.put("answerSpecJson", "{\"answer\":\"a\"}");
        snapshot.put("explanation", "private explanation");
        snapshot.put("unexpectedPrivateField", "private");

        var json = new ObjectMapper().valueToTree(GameItemResult.from(snapshot));

        assertEquals("Choose the answer", json.path("prompt").asText());
        assertEquals(1, json.path("options").size());
        assertFalse(json.has("answerSpecJson"));
        assertFalse(json.has("explanation"));
        assertFalse(json.has("unexpectedPrivateField"));
    }
}
