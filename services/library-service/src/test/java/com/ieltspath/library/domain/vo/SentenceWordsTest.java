package com.ieltspath.library.domain.vo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The word list must match the player's, or a pronunciation lands on the wrong tile. */
class SentenceWordsTest {
    @Test
    void splitsLowerCasesAndDropsPunctuation() {
        assertEquals(List.of("it's", "the", "perfect", "late", "night", "snack"),
                SentenceWords.normalized("It's the perfect late night snack."));
        assertEquals(List.of("i", "am", "going", "to", "tell", "you", "about", "my", "weekend"),
                SentenceWords.normalized("I am going to tell you about my weekend."));
    }

    @Test
    void makesTypographicApostrophesStraightAndTrimsQuotes() {
        assertEquals(List.of("don't", "cortés's"), SentenceWords.normalized("Don’t Cortés’s"));
        assertEquals(List.of("stand", "up", "straight"), SentenceWords.normalized("\"Stand up straight!\""));
        assertEquals(List.of("goat", "herd"), SentenceWords.normalized("'goat' 'herd'"));
    }

    @Test
    void hyphensAndDashesSeparateWordsAndEmptyWordsAreSkipped() {
        assertEquals(List.of("hard", "headed", "evidence", "based"), SentenceWords.normalized("hard-headed, evidence–based"));
        assertEquals(List.of("one", "two"), SentenceWords.normalized("one — two"));
        assertEquals(List.of("700"), SentenceWords.normalized("7:00."));
        assertTrue(SentenceWords.normalized("  ...  ").isEmpty());
        assertTrue(SentenceWords.normalized(null).isEmpty());
    }
}
