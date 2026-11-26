package com.ieltspath.assessment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.assessment.application.port.PlacementSpeakingGradeStore;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcPlacementSpeakingGradeStore;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.events.assessment.SpeakingGradingRequestedV1;
import com.ieltspath.events.assessment.SpeakingGradingCompletedV1;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Testcontainers(disabledWithoutDocker = true)
class PlacementSpeakingTimeoutIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc; static PlacementSpeakingGradeStore grades;

    @BeforeAll static void setup() {
        var source=new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(source).load().migrate(); jdbc=new JdbcTemplate(source);
        grades=new JdbcPlacementSpeakingGradeStore(jdbc,new ObjectMapper().registerModule(new JavaTimeModule()),mock(OutboxTraceContext.class));
    }

    @Test void tenMinuteDeadlineMarksOnePendingPlacementGradeFailed() {
        UUID attempt=UUID.randomUUID(),user=UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status) VALUES(?,?,?,'PLACEMENT','STANDARD','WEB','SUBMITTED')",
                attempt,user,UUID.randomUUID());
        Instant now=Instant.now();
        var request=new SpeakingGradingRequestedV1(UUID.randomUUID(),SpeakingGradingRequestedV1.EVENT_TYPE,now,
                SpeakingGradingRequestedV1.SOURCE,new SpeakingGradingRequestedV1.Data(attempt,user,
                List.of(new SpeakingGradingRequestedV1.Item(UUID.randomUUID(),1,1,"Question","speaking/"+user+"/one.wav"))));
        grades.enqueue(request,now.minusSeconds(1));

        assertEquals(List.of(attempt),grades.failExpired(now,50));
        assertEquals("FAILED",grades.find(attempt).orElseThrow().status());
        assertEquals(List.of(),grades.failExpired(now,50));
    }

    @Test void duplicateCompletionEventIsConsumedOnce() {
        UUID attempt=UUID.randomUUID(),user=UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status) VALUES(?,?,?,'PLACEMENT','STANDARD','WEB','SUBMITTED')",
                attempt,user,UUID.randomUUID());
        Instant now=Instant.now();
        var request=new SpeakingGradingRequestedV1(UUID.randomUUID(),SpeakingGradingRequestedV1.EVENT_TYPE,now,
                SpeakingGradingRequestedV1.SOURCE,new SpeakingGradingRequestedV1.Data(attempt,user,
                List.of(new SpeakingGradingRequestedV1.Item(UUID.randomUUID(),1,1,"Question","speaking/"+user+"/one.wav"))));
        grades.enqueue(request,now.plusSeconds(600));
        var completed=new SpeakingGradingCompletedV1(UUID.randomUUID(),SpeakingGradingCompletedV1.EVENT_TYPE,now,
                SpeakingGradingCompletedV1.SOURCE,new SpeakingGradingCompletedV1.Data(attempt,user,"GRADED",
                new java.math.BigDecimal("6.5"),List.of(),"Nhận xét.","SPEAKING",1,null));

        assertTrue(grades.complete(completed));
        assertFalse(grades.complete(completed));
        assertEquals("GRADED",grades.find(attempt).orElseThrow().status());
    }
}
