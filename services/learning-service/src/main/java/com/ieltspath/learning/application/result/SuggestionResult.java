package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Optional things worth doing next, at most five. {@code type}: PRACTICE_SET, PRACTICE_TEST or LESSON. {@code reason}:
 * CONSOLIDATE, DUE or NEXT_IN_TREE. {@code openPath} is the API route that starts it, or null when the item is opened
 * from its own list (a practice test is assigned through its course).
 */
public record SuggestionResult(List<Item> suggestions) {
    public record Item(String type, UUID id, String title, UUID knowledgePointId, String knowledgePointCode,
                       String reason, BigDecimal band, String skill, String openPath) {}
}
