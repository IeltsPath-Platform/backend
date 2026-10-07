package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.exception.PracticeAttemptAlreadySubmittedException;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PracticeAttemptTest {
    @Test
    void sameRequestReplaysAndDifferentRequestCannotSubmitTwice() {
        UUID id = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        var attempt = PracticeAttempt.start(id, UUID.randomUUID(), UUID.randomUUID(), LearningSkill.READING,
                UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        var response = new PracticeSubmission(id, 1, 1, 1.0, true, true, List.of(), null, List.of());
        assertTrue(attempt.acceptsAnswers(request));
        attempt.recordResult(request, response, Instant.now());
        assertFalse(attempt.acceptsAnswers(request));
        assertSame(response, attempt.response());
        assertThrows(PracticeAttemptAlreadySubmittedException.class,
                () -> attempt.acceptsAnswers(UUID.randomUUID()));
    }
}
