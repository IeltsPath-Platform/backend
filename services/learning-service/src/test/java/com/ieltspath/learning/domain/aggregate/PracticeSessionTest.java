package com.ieltspath.learning.domain.aggregate;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PracticeSessionTest {
    private final UUID user = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-11-09T10:00:00Z");
    private final PracticeSession session = PracticeSession.start(user, "WRITING", UUID.randomUUID(), now);

    @Test
    void sessionIsUsableOnlyForItsOwnerKindAndBeforeExpiry() {
        assertTrue(session.isUsableFor(user, "WRITING", now));
        assertFalse(session.isUsableFor(UUID.randomUUID(), "WRITING", now));
        assertFalse(session.isUsableFor(user, "SPEAKING", now));
        assertFalse(session.isUsableFor(user, "WRITING", session.expiresAt()));
    }

    @Test
    void sessionExpiresSixHoursAfterCreation() {
        assertEquals(now.plusSeconds(6 * 60 * 60), session.expiresAt());
    }
}
