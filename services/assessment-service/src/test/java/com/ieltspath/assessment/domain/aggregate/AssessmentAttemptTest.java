package com.ieltspath.assessment.domain.aggregate;

import com.ieltspath.assessment.domain.exception.AttemptExpiredException;
import com.ieltspath.assessment.domain.vo.*;
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
    @Test void lateSubmitExpiresTheAttemptAndEverySubmitAfterThatFailsTheSameWay(){
        var attempt=AssessmentAttempt.start(UUID.randomUUID(),UUID.randomUUID(),AttemptType.QUIZ,AttemptMode.STANDARD,AttemptChannel.API,Instant.now().minusSeconds(1));
        assertThrows(AttemptExpiredException.class,()->attempt.submit(Instant.now())); assertEquals(AttemptStatus.EXPIRED,attempt.getStatus());
        assertThrows(AttemptExpiredException.class,()->attempt.submit(Instant.now())); assertNull(attempt.getSubmittedAt());
    }
    @Test void practiceTestDeadlineIsEnforcedAfterAShortGrace(){
        var late=AssessmentAttempt.start(UUID.randomUUID(),UUID.randomUUID(),AttemptType.OFFICIAL_PRACTICE,AttemptMode.TIMED,AttemptChannel.WEB,Instant.now().minusSeconds(120));
        assertTrue(late.isPastDeadline(Instant.now()));
        assertThrows(AttemptExpiredException.class,()->late.submit(Instant.now())); assertEquals(AttemptStatus.EXPIRED,late.getStatus());

        var justOver=AssessmentAttempt.start(UUID.randomUUID(),UUID.randomUUID(),AttemptType.OFFICIAL_PRACTICE,AttemptMode.TIMED,AttemptChannel.WEB,Instant.now().minusSeconds(5));
        assertFalse(justOver.isPastDeadline(Instant.now()));
        justOver.submit(Instant.now()); assertEquals(AttemptStatus.SUBMITTED,justOver.getStatus());
    }
    @Test void attemptWithoutDeadlineIsNeverLate(){
        var attempt=AssessmentAttempt.start(UUID.randomUUID(),UUID.randomUUID(),AttemptType.OFFICIAL_PRACTICE,AttemptMode.STANDARD,AttemptChannel.WEB,null);
        assertFalse(attempt.isPastDeadline(Instant.now().plusSeconds(86_400)));
    }
    @Test void typeComesFromTheContentPackage(){
        assertEquals(AttemptType.TOPIC_GATE,AttemptType.forContentPackageType("TOPIC_TEST"));
        assertEquals(AttemptType.MOCK,AttemptType.forContentPackageType("MOCK_TEST"));
        assertEquals(AttemptType.PLACEMENT,AttemptType.forContentPackageType("PLACEMENT_TEST"));
        assertEquals(AttemptType.QUIZ,AttemptType.forContentPackageType("QUIZ"));
        assertEquals(AttemptType.COURSE_GATE, AttemptType.forContentPackageType("COURSE_TEST"));
    }
}
