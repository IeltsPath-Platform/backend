package com.ieltspath.assessment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.BandStatisticsStore;
import com.ieltspath.assessment.application.usecase.GetBandHistoryUseCase;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.vo.BandMeasurement;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcBandStatisticsStore;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.Instant;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Testcontainers(disabledWithoutDocker=true)
class BandStatisticsPersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static JdbcBandStatisticsStore store;
    private final UUID user=UUID.randomUUID();
    private final Instant from=Instant.parse("2026-10-01T00:00:00Z"),until=Instant.parse("2026-11-01T00:00:00Z");
    @BeforeAll static void setup() {
        var ds=new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc=new JdbcTemplate(ds);store=new JdbcBandStatisticsStore(new NamedParameterJdbcTemplate(ds),mock(EntityManager.class),new ObjectMapper());
    }
    private UUID attempt(UUID owner,Instant submitted) {
        UUID id=UUID.randomUUID();
        jdbc.update("""
            INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status,submitted_at)
            VALUES(?,?,?,'PLACEMENT','STANDARD','WEB','SUBMITTED',?)
            """,id,owner,UUID.randomUUID(),Timestamp.from(submitted));
        return id;
    }
    private UUID result(UUID attempt,int version,String status,Double band,String quality) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_results(id,attempt_id,result_version,status) VALUES(?,?,?,?)",id,attempt,version,status);
        if(quality!=null) store.freeze(id,List.of(new BandMeasurement("LISTENING",band,"AUTO",quality,"placement-v1",null)));
        return id;
    }
    private BandStatisticsStore.Page history(BandStatisticsStore.Cursor cursor,int limit) { return store.history(user,from,until,"PLACEMENT","LISTENING",cursor,limit); }
    @Test void vietnamMorningBelongsToVietnamDateWithInclusiveStartAndExclusiveEnd() {
        Instant midnight=Instant.parse("2026-10-14T17:00:00Z");
        UUID start=attempt(user,midnight);
        UUID morning=attempt(user,Instant.parse("2026-10-14T23:30:00Z"));
        result(start,1,"COMPLETED",5.0,"MEASURED");
        result(morning,1,"COMPLETED",6.0,"MEASURED");
        result(attempt(user,midnight.minusSeconds(1)),1,"COMPLETED",4.0,"MEASURED");
        result(attempt(user,Instant.parse("2026-10-15T17:00:00Z")),1,"COMPLETED",7.0,"MEASURED");
        var useCase=new GetBandHistoryUseCase(store,mock(AssessmentAttemptRepository.class));
        LocalDate day=LocalDate.of(2026,10,15);
        var today=useCase.execute(user,day,day,"PLACEMENT","LISTENING",null,100,"Asia/Ho_Chi_Minh");
        assertEquals(List.of(morning,start),today.points().stream().map(BandStatisticsStore.Point::attemptId).toList());
        assertEquals(5.0,today.baseline());
        assertEquals(6.0,today.latest());
        var previous=useCase.execute(user,day.minusDays(1),day.minusDays(1),"PLACEMENT","LISTENING",null,100,"Asia/Ho_Chi_Minh");
        assertEquals(1,previous.points().size());
        assertFalse(previous.points().stream().anyMatch(point->point.attemptId().equals(morning)));
    }
    @Test void newestCompletedReplacesOlderWhilePendingVersionRetainsMeasuredResult() {
        UUID attempt=attempt(user,from.plusSeconds(3600));result(attempt,1,"COMPLETED",6.0,"MEASURED");
        UUID pending=result(attempt,2,"PROCESSING",7.0,"MEASURED");
        assertEquals(6.0,history(null,100).points().getFirst().band());assertTrue(history(null,100).points().getFirst().pendingRegrade());
        jdbc.update("UPDATE assessment_results SET status='COMPLETED' WHERE id=?",pending);
        assertEquals(1,history(null,100).points().size());assertEquals(7.0,history(null,100).latest());
        assertFalse(history(null,100).points().getFirst().pendingRegrade());
    }
    @Test void excludesLegacyFallbackOtherOwnersAndOutOfRangeWithoutRevivingOldVersion() {
        UUID replaced=attempt(user,from.plusSeconds(3600));result(replaced,1,"COMPLETED",6.0,"MEASURED");result(replaced,2,"COMPLETED",5.5,"FALLBACK");
        result(attempt(user,from.plusSeconds(7200)),1,"COMPLETED",6.5,null);
        result(attempt(UUID.randomUUID(),from.plusSeconds(3600)),1,"COMPLETED",9.0,"MEASURED");
        result(attempt(user,until),1,"COMPLETED",9.0,"MEASURED");
        assertTrue(history(null,100).points().isEmpty());assertNull(history(null,100).latest());
    }
    @Test void cursorPagesPreserveWholeRangeSummaryAndFrozenSnapshot() {
        UUID oldest=attempt(user,from.plusSeconds(3600));UUID firstResult=result(oldest,1,"COMPLETED",5.0,"MEASURED");
        UUID newest=attempt(user,from.plusSeconds(7200));result(newest,1,"COMPLETED",7.0,"MEASURED");
        store.freeze(firstResult,List.of(new BandMeasurement("LISTENING",9.0,"AUTO","MEASURED","changed",null)));
        var page=history(null,1);assertEquals(newest,page.points().getFirst().attemptId());assertEquals(5.0,page.baseline());assertEquals(7.0,page.latest());
        var tail=history(new BandStatisticsStore.Cursor(page.points().getFirst().submittedAt(),newest),1);
        assertEquals(oldest,tail.points().getFirst().attemptId());assertEquals(5.0,tail.points().getFirst().band());assertEquals(7.0,tail.latest());
    }
}
