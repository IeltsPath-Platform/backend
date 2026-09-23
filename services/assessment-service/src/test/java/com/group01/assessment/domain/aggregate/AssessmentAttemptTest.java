package com.group01.assessment.domain.aggregate;

import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AssessmentAttemptTest {
    @Test void startsInProgressAndCanBeSubmitted(){
        var attempt=AssessmentAttempt.start(UUID.randomUUID(),UUID.randomUUID(),AttemptType.MOCK,AttemptMode.TIMED,AttemptChannel.WEB,Instant.now().plusSeconds(60));
        attempt.submit(Instant.now());
        assertEquals(AttemptStatus.SUBMITTED,attempt.getStatus()); assertNotNull(attempt.getSubmittedAt());
    }
    @Test void expiredAttemptCannotBeSubmitted(){
        var attempt=AssessmentAttempt.start(UUID.randomUUID(),UUID.randomUUID(),AttemptType.QUIZ,AttemptMode.STANDARD,AttemptChannel.API,Instant.now().minusSeconds(1));
        assertThrows(InvalidAssessmentStateException.class,()->attempt.submit(Instant.now())); assertEquals(AttemptStatus.EXPIRED,attempt.getStatus());
    }
}
