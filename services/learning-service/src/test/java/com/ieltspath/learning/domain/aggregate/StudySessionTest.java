package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.exception.StudyTrackingException;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StudySessionTest {
    private final Instant start=Instant.parse("2026-10-14T16:59:50Z");
    private StudySession session() {
        return StudySession.start(UUID.randomUUID(),UUID.randomUUID(),"LESSON",UUID.randomUUID(),
                UUID.randomUUID(),"Asia/Ho_Chi_Minh",start);
    }
    private StudySession.Interval interval(long from,long to) { return new StudySession.Interval(from,to); }
    @Test void vocabularyReviewIsNotATrackedStudySource() {
        assertThrows(IllegalArgumentException.class,()->StudySession.start(UUID.randomUUID(),UUID.randomUUID(),
                "VOCABULARY_REVIEW",UUID.randomUUID(),null,"UTC",start));
    }
    @Test void splitsAtLocalMidnightAndPreservesTotal() {
        var result=session().accept(1,1,List.of(interval(0,30000)),start.plusSeconds(30),false);
        assertEquals(30000,result.acceptedMs());
        assertEquals(2,result.segments().size());
        assertEquals(LocalDate.parse("2026-10-14"),result.segments().getFirst().date());
        assertEquals(10000,result.segments().getFirst().milliseconds());
        assertEquals(20000,result.segments().getLast().milliseconds());
    }
    @Test void overlappingPreviousIntervalsOnlyCountTheNewTail() {
        var first=session().accept(1,1,List.of(interval(0,30000)),start.plusSeconds(30),false);
        var second=first.session().accept(1,2,List.of(interval(10000,60000)),start.plusSeconds(60),false);
        assertEquals(30000,second.acceptedMs());
        assertEquals(60000,second.session().totalAcceptedMs());
    }
    @Test void rejectsExpiredFutureBacklogAndUnorderedIntervals() {
        assertThrows(StudyTrackingException.class,()->session().accept(1,1,List.of(),start.plusSeconds(90),false));
        assertThrows(IllegalArgumentException.class,()->session().accept(1,1,List.of(interval(0,30001)),start.plusSeconds(30),false));
        assertThrows(IllegalArgumentException.class,()->session().accept(1,1,List.of(interval(100,200),interval(50,300)),start.plusSeconds(30),false));
        var renewed=session().accept(1,1,List.of(),start.plusSeconds(80),false).session();
        assertThrows(IllegalArgumentException.class,()->renewed.accept(1,2,List.of(interval(0,30000)),start.plusSeconds(100),false));
    }
    @Test void closedSessionCannotResurrectAndEpochSequenceMustMatch() {
        var s=session();
        assertThrows(StudyTrackingException.class,()->s.accept(2,1,List.of(),start.plusSeconds(1),false));
        assertThrows(StudyTrackingException.class,()->s.accept(1,2,List.of(),start.plusSeconds(1),false));
        var closed=s.accept(1,1,List.of(interval(0,1000)),start.plusSeconds(1),true).session();
        assertThrows(StudyTrackingException.class,()->closed.accept(1,2,List.of(),start.plusSeconds(2),false));
    }
    @Test void emptyIdleHeartbeatRenewsLeaseWithoutAddingTime() {
        var result=session().accept(1,1,List.of(),start.plusSeconds(30),false);
        assertEquals(0,result.acceptedMs());
        assertEquals(start.plusSeconds(120),result.session().leaseExpiresAt());
    }
    @Test void daylightSavingMidnightUsesActualZoneBoundary() {
        Instant anchor=Instant.parse("2026-03-09T03:59:50Z");
        var s=StudySession.start(UUID.randomUUID(),UUID.randomUUID(),"LESSON",UUID.randomUUID(),null,"America/New_York",anchor);
        var result=s.accept(1,1,List.of(interval(0,30000)),anchor.plusSeconds(30),false);
        assertEquals(LocalDate.parse("2026-03-08"),result.segments().getFirst().date());
        assertEquals(10000,result.segments().getFirst().milliseconds());
    }
    @Test void databaseRoundTripStartPrecisionKeepsValidMillisecondBoundaryInterval() {
        Instant clockValue=Instant.parse("2026-10-14T16:59:50.123456789Z");
        var started=StudySession.start(UUID.randomUUID(),UUID.randomUUID(),"LESSON",UUID.randomUUID(),
                UUID.randomUUID(),"Asia/Ho_Chi_Minh",clockValue);
        Instant databaseReadBack=started.startedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var restored=new StudySession(started.id(),started.userId(),started.clientSessionId(),started.sourceType(),
                started.sourceId(),started.courseId(),started.timezone(),started.epoch(),databaseReadBack,
                started.leaseExpiresAt(),started.sequence(),started.watermarkMs(),started.totalAcceptedMs(),started.status());

        var accepted=restored.accept(1,1,List.of(interval(0,30_000)),
                clockValue.plusMillis(30_000),false);

        assertEquals(30_000,accepted.acceptedMs());
        assertEquals(databaseReadBack,accepted.session().startedAt());
    }

    @Test void subMillisecondMidnightSplitKeepsPositivePersistableDuration() {
        Instant anchor=Instant.parse("2026-10-14T16:59:59.999500Z");
        var started=StudySession.start(UUID.randomUUID(),UUID.randomUUID(),"LESSON",UUID.randomUUID(),
                null,"Asia/Ho_Chi_Minh",anchor);

        var accepted=started.accept(1,1,List.of(interval(0,1)),anchor.plusMillis(1),false);

        assertEquals(1,accepted.acceptedMs());
        assertEquals(1,accepted.segments().stream().mapToLong(StudySession.Segment::milliseconds).sum());
        assertTrue(accepted.segments().stream().allMatch(segment -> segment.milliseconds() > 0));
        assertEquals(LocalDate.parse("2026-10-15"),accepted.segments().getFirst().date());
    }
}
