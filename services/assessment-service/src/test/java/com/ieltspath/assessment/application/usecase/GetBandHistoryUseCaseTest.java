package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.BandStatisticsStore;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetBandHistoryUseCaseTest {
    private final BandStatisticsStore store=mock(BandStatisticsStore.class);
    private final AssessmentAttemptRepository attempts=mock(AssessmentAttemptRepository.class);
    private final GetBandHistoryUseCase useCase=new GetBandHistoryUseCase(store,attempts);
    private final UUID user=UUID.randomUUID();
    private final LocalDate from=LocalDate.of(2026,10,1),to=LocalDate.of(2026,10,31);
    @Test void invalidRangesAndCursorFailBeforeQuery() {
        assertThrows(IllegalArgumentException.class,()->useCase.execute(user,from,from.plusDays(366),"PLACEMENT","OVERALL",null,100,"Asia/Ho_Chi_Minh"));
        assertThrows(IllegalArgumentException.class,()->useCase.execute(user,from,to,"TOPIC_GATE","OVERALL",null,100,"Asia/Ho_Chi_Minh"));
        assertThrows(IllegalArgumentException.class,()->useCase.execute(user,from,to,"PLACEMENT","OVERALL","invalid",100,"Asia/Ho_Chi_Minh"));
        verifyNoInteractions(store);
    }
    @Test void summaryIsNotRecomputedFromOneVisiblePage() {
        Instant at=Instant.parse("2026-10-15T00:00:00Z");
        var points=List.of(new BandStatisticsStore.Point(UUID.randomUUID(),UUID.randomUUID(),2,at,at,7,true,"AUTO","PLACEMENT_ESTIMATE"),
                new BandStatisticsStore.Point(UUID.randomUUID(),UUID.randomUUID(),1,at.minusSeconds(60),at.minusSeconds(60),6,false,"AUTO","PLACEMENT_ESTIMATE"));
        when(store.history(eq(user),any(),any(),eq("PLACEMENT"),eq("LISTENING"),isNull(),eq(2)))
                .thenReturn(new BandStatisticsStore.Page(points,4.0,7.0,at,2));
        var response=useCase.execute(user,from,to,"PLACEMENT","LISTENING",null,1,"Asia/Ho_Chi_Minh");
        assertEquals(1,response.points().size());assertNotNull(response.nextCursor());assertEquals(3.0,response.delta());assertEquals(4.0,response.baseline());
    }
    @Test void invalidTimezonesFailBeforeQuery() {
        for(String timezone:Arrays.asList("Invalid/Zone","+07:00","Asia/No_Such_City","",null)) {
            assertThrows(IllegalArgumentException.class,()->useCase.execute(user,from,to,"PLACEMENT","OVERALL",null,100,timezone));
        }
        verifyNoInteractions(store);
    }
    @Test void vietnamDayUsesLocalMidnightAndLocalDataFrom() {
        LocalDate day=LocalDate.of(2026,10,15);
        Instant submitted=Instant.parse("2026-10-14T23:30:00Z");
        when(store.history(eq(user),any(),any(),eq("PLACEMENT"),eq("LISTENING"),isNull(),eq(101)))
                .thenReturn(new BandStatisticsStore.Page(List.of(),6.0,6.0,submitted,1));
        var response=useCase.execute(user,day,day,"PLACEMENT","LISTENING",null,100,"Asia/Ho_Chi_Minh");
        verify(store).history(user,Instant.parse("2026-10-14T17:00:00Z"),Instant.parse("2026-10-15T17:00:00Z"),
                "PLACEMENT","LISTENING",null,101);
        assertEquals(day,response.dataFrom());
    }
    @Test void daylightSavingDayUsesNextLocalMidnightRatherThanTwentyFourHours() {
        LocalDate day=LocalDate.of(2026,3,8);
        when(store.history(eq(user),any(),any(),eq("PLACEMENT"),eq("LISTENING"),isNull(),eq(101)))
                .thenReturn(new BandStatisticsStore.Page(List.of(),null,null,null,0));
        useCase.execute(user,day,day,"PLACEMENT","LISTENING",null,100,"America/New_York");
        verify(store).history(user,Instant.parse("2026-03-08T05:00:00Z"),Instant.parse("2026-03-09T04:00:00Z"),
                "PLACEMENT","LISTENING",null,101);
    }
    @Test void gateContextsDoNotInventCourseOwnershipFromPackageId() {
        for(AttemptType type:List.of(AttemptType.TOPIC_GATE,AttemptType.COURSE_GATE)) {
            var attempt=AssessmentAttempt.start(user,UUID.randomUUID(),type,AttemptMode.STANDARD,AttemptChannel.WEB,null);
            when(attempts.findByIdAndUserId(attempt.getId(),user)).thenReturn(Optional.of(attempt));
            var context=useCase.context(user,"ASSESSMENT",attempt.getId());
            assertEquals(attempt.getId(),context.sourceId());
            assertNull(context.courseId());
        }
    }
    @Test void contextRequiresOwnerAndLiveAttempt() {
        UUID id=UUID.randomUUID();when(attempts.findByIdAndUserId(id,user)).thenReturn(Optional.empty());
        assertThrows(AssessmentNotFoundException.class,()->useCase.context(user,"ASSESSMENT",id));
        var attempt=AssessmentAttempt.start(user,UUID.randomUUID(),AttemptType.PLACEMENT,AttemptMode.STANDARD,AttemptChannel.WEB,Instant.now().minusSeconds(1));
        when(attempts.findByIdAndUserId(id,user)).thenReturn(Optional.of(attempt));
        assertThrows(AssessmentNotFoundException.class,()->useCase.context(user,"ASSESSMENT",id));
    }
}
