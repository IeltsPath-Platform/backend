package com.ieltspath.community.domain.vo;

import com.ieltspath.community.domain.exception.CommunityException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeedCursorTest {

    private static String encoded(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void encodeThenParseGivesTheSameCursor() {
        var cursor = new FeedCursor(Instant.parse("2026-10-16T10:00:00Z"), UUID.randomUUID());
        assertEquals(cursor, FeedCursor.parse(cursor.encode()));
    }

    @Test
    void nullOrBlankTextMeansNoCursor() {
        assertNull(FeedCursor.parse(null));
        assertNull(FeedCursor.parse("  "));
    }

    @Test
    void malformedCursorsAreRejected() {
        String id = UUID.randomUUID().toString();
        String[] bad = {
                encoded("2026-10-16T10:00:00Z"),
                encoded("not-a-date|" + id),
                encoded("2026-10-16T10:00:00Z|not-a-uuid"),
                encoded("2026-10-16T10:00:00Z|" + id + "|extra"),
                "x".repeat(201)
        };
        for (String value : bad) {
            var error = assertThrows(CommunityException.class, () -> FeedCursor.parse(value));
            assertEquals("Invalid cursor", error.getMessage());
        }
    }
}
