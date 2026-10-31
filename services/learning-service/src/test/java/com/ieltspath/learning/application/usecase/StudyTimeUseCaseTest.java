package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.StudySourceVerifier;
import com.ieltspath.learning.application.port.StudyTimezones;
import com.ieltspath.learning.domain.aggregate.StudySession;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import com.ieltspath.learning.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StudyTimeUseCaseTest {
    final UUID user=UUID.randomUUID(),client=UUID.randomUUID(),source=UUID.randomUUID();
    final Instant now=Instant.parse("2026-10-15T10:00:00Z");
    final Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    final StudySessionRepository sessions=mock(StudySessionRepository.class);
    final StudyTimezones timezones=id->ZoneId.of("Asia/Ho_Chi_Minh");
    final ScheduleStudyGoalUseCase goalSchedule=mock(ScheduleStudyGoalUseCase.class);
    final StudySourceVerifier verifier=mock(StudySourceVerifier.class);
    final TransactionTemplate tx=new TransactionTemplate() {
        @Override public <T> T execute(TransactionCallback<T> action) { return action.doInTransaction(new SimpleTransactionStatus()); }
    };
    @Test void startValidatesOutsideLockAndRefusesCompetingLease() {
        var active=StudySession.start(user,UUID.randomUUID(),"LESSON",source,null,"UTC",now);
        when(verifier.verify(user,"LESSON",source)).thenReturn(new StudySourceVerifier.Context(source,null));
        when(sessions.findOpen(user)).thenReturn(Optional.of(active));
        var useCase=new StartStudySessionUseCase(sessions,timezones,goalSchedule,verifier,tx,clock);
        assertThrows(StudyTrackingException.class,()->useCase.execute(user,client,"LESSON",source,false));
        var order=inOrder(verifier,sessions);
        order.verify(verifier).verify(user,"LESSON",source);
        order.verify(sessions).lockUser(user);
        verify(sessions,never()).save(any());
    }
    @Test void explicitTakeoverClosesPreviousSessionAndStartsAtCurrentServerTime() {
        var active=StudySession.start(user,UUID.randomUUID(),"LESSON",source,null,"UTC",now.minusSeconds(10));
        when(verifier.verify(user,"LESSON",source)).thenReturn(new StudySourceVerifier.Context(source,null));
        when(sessions.findOpen(user)).thenReturn(Optional.of(active));
        var result=new StartStudySessionUseCase(sessions,timezones,goalSchedule,verifier,tx,clock).execute(user,client,"LESSON",source,true);
        assertEquals(now,result.serverStartedAt());
        var order=inOrder(sessions,goalSchedule);
        order.verify(sessions).lockUser(user);
        order.verify(goalSchedule).rescheduleIfTimezoneChanged(user);
        verify(sessions).save(active.closeAt(now));
    }
    @Test void startReplayNeverReopensClosedSessionOrCallsOwner() {
        var closed=StudySession.start(user,client,"LESSON",source,null,"UTC",now).closeAt(now);
        when(sessions.findByClient(user,client)).thenReturn(Optional.of(closed));
        var useCase=new StartStudySessionUseCase(sessions,timezones,goalSchedule,verifier,tx,clock);
        assertEquals("CLOSED",useCase.execute(user,client,"LESSON",source,false).status());
        assertThrows(StudyTrackingException.class,()->useCase.execute(user,client,"REVIEW",source,false));
        verifyNoInteractions(verifier,goalSchedule);
    }
    @Test void lostAcknowledgementReplaysReceiptWithoutWritingAgainAndRejectsChangedPayload() {
        var session=StudySession.start(user,client,"LESSON",source,null,"UTC",now.minusSeconds(30));
        when(sessions.find(user,session.id())).thenReturn(Optional.of(session));
        var heartbeat=new RecordStudyHeartbeatUseCase(sessions,clock);
        var intervals=List.of(new StudySession.Interval(0,30000));
        var first=heartbeat.execute(user,session.id(),1,1,intervals,true);
        var captor=org.mockito.ArgumentCaptor.forClass(StudySessionRepository.Receipt.class);
        verify(sessions).record(any(),captor.capture(),any());
        when(sessions.receipt(session.id(),1,1)).thenReturn(Optional.of(captor.getValue()));
        assertEquals(first,heartbeat.execute(user,session.id(),1,1,intervals,true));
        assertThrows(StudyTrackingException.class,()->heartbeat.execute(user,session.id(),1,1,List.of(),true));
        verify(sessions,times(1)).record(any(),any(),any());
    }
    @Test void foreignSessionNeverReadsReceipt() {
        assertThrows(StudyTrackingException.class,()->new RecordStudyHeartbeatUseCase(sessions,clock)
                .execute(user,UUID.randomUUID(),1,1,List.of(),false));
        verify(sessions,never()).receipt(any(),anyLong(),anyLong());
    }
    @Test void statisticsDistinguishUnknownZeroFutureAndHistoricalTargets() {
        when(sessions.trackingStartedAt()).thenReturn(Instant.parse("2026-10-12T00:00:00Z"));
        when(sessions.goals(eq(user),any(),any())).thenReturn(List.of(
                new StudySessionRepository.Goal(LocalDate.parse("2026-10-12"),30),
                new StudySessionRepository.Goal(LocalDate.parse("2026-10-15"),60)));
        var result=new GetStudyTimeStatisticsUseCase(sessions,timezones,clock)
                .execute(user,LocalDate.parse("2026-10-11"),LocalDate.parse("2026-10-16"),"DAY",null);
        assertNull(result.days().getFirst().actualSeconds());
        assertEquals("NOT_TRACKED",result.days().getFirst().status());
        assertEquals(0L,result.days().get(2).actualSeconds());
        assertEquals(1800L,result.days().get(2).plannedSeconds());
        assertEquals(3600L,result.days().get(4).plannedSeconds());
        assertNull(result.days().getLast().actualSeconds());
    }
    @Test void courseScopeDoesNotCopyGlobalGoalAndOversizedRangeFailsBeforeQuery() {
        when(sessions.trackingStartedAt()).thenReturn(now.minusSeconds(86400));
        var useCase=new GetStudyTimeStatisticsUseCase(sessions,timezones,clock);
        var today=LocalDate.parse("2026-10-15");
        assertNull(useCase.execute(user,today,today,"DAY",UUID.randomUUID()).days().getFirst().plannedSeconds());
        verify(sessions,never()).goals(any(),any(),any());
        assertThrows(IllegalArgumentException.class,()->useCase.execute(user,today.minusDays(366),today,"DAY",null));
    }
    @Test void weekAndMonthSumMillisecondsBeforeConvertingToSeconds() {
        LocalDate first=LocalDate.parse("2026-10-12"), last=first.plusDays(1);
        when(sessions.trackingStartedAt()).thenReturn(Instant.parse("2026-10-11T00:00:00Z"));
        when(sessions.daily(user,first,last,"ALL")).thenReturn(List.of(
                new StudySessionRepository.Daily(first,"UTC",600),
                new StudySessionRepository.Daily(last,"Asia/Ho_Chi_Minh",600)));
        when(sessions.goals(user,first,last)).thenReturn(List.of(new StudySessionRepository.Goal(first,30)));
        var useCase=new GetStudyTimeStatisticsUseCase(sessions,timezones,clock);

        assertTrue(useCase.execute(user,first,last,"DAY",null).days().stream()
                .allMatch(day->day.actualSeconds()==0L));
        for(String grain:List.of("WEEK","MONTH")) {
            var bucket=useCase.execute(user,first,last,grain,null).days().getFirst();
            assertEquals(1L,bucket.actualSeconds());
            assertEquals(3600L,bucket.plannedSeconds());
            assertEquals("MULTIPLE",bucket.timezone());
            assertEquals("COMPLETE",bucket.status());
        }
    }
}
