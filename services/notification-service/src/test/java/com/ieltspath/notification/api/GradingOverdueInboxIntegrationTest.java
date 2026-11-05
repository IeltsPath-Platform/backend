package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUser;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.role.CanonicalRoles;
import com.ieltspath.events.assessment.GradingOverdueV1;
import com.ieltspath.notification.api.exception.GlobalExceptionHandler;
import com.ieltspath.notification.application.NotificationInboxUseCase;
import com.ieltspath.notification.application.NotificationPreferenceService;
import com.ieltspath.notification.application.RecordGradingOverdueUseCase;
import com.ieltspath.notification.infrastructure.persistence.NotificationPreferenceRepositoryAdapter;
import com.ieltspath.notification.infrastructure.persistence.NotificationRepositoryAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The late-examiner notice is stored once and shows up in the learner's own inbox with the job it is about. */
@Testcontainers(disabledWithoutDocker = true)
class GradingOverdueInboxIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static JdbcTemplate jdbc;
    static RecordGradingOverdueUseCase record;
    static NotificationRepositoryAdapter repository;

    @BeforeAll
    static void setupDatabase() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("filesystem:src/main/resources/db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        repository = new NotificationRepositoryAdapter(jdbc);
        record = new RecordGradingOverdueUseCase(repository);
    }

    @BeforeEach
    void cleanUp() {
        jdbc.execute("DELETE FROM notifications");
    }

    private MockMvc inboxOf(UUID callerId) {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(callerId, Set.of(CanonicalRoles.CUSTOMER)));
        var controller = new NotificationController(new NotificationInboxUseCase(repository), users,
                new NotificationPreferenceService(new NotificationPreferenceRepositoryAdapter(jdbc)));
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private GradingOverdueV1 overdue(UUID learner, UUID examiner, UUID job) {
        return new GradingOverdueV1(UUID.randomUUID(), GradingOverdueV1.EVENT_TYPE, Instant.now(), learner, examiner, job,
                "WRITING", Instant.now().minusSeconds(60));
    }

    @Test
    void theLearnerSeesTheNoticeWithTheJobAndTheExaminerAndNobodyElseDoes() throws Exception {
        UUID learner = UUID.randomUUID(), examiner = UUID.randomUUID(), job = UUID.randomUUID();
        var event = overdue(learner, examiner, job);
        assertTrue(record.execute(event, event.eventType(), event.eventId().toString()).isPresent());

        inboxOf(learner).perform(get("/api/notifications"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("assessment.grading-overdue.v1"))
                .andExpect(jsonPath("$[0].gradingJobId").value(job.toString()))
                .andExpect(jsonPath("$[0].actorId").value(examiner.toString()))
                .andExpect(jsonPath("$[0].postId").doesNotExist())
                .andExpect(jsonPath("$[0].readAt").doesNotExist());
        inboxOf(learner).perform(get("/api/notifications/unread-count")).andExpect(jsonPath("$.count").value(1));
        inboxOf(UUID.randomUUID()).perform(get("/api/notifications")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theSameEventDeliveredTwiceNotifiesOnce() {
        UUID learner = UUID.randomUUID();
        var event = overdue(learner, UUID.randomUUID(), UUID.randomUUID());

        assertTrue(record.execute(event, event.eventType(), event.eventId().toString()).isPresent());
        assertTrue(record.execute(event, event.eventType(), event.eventId().toString()).isEmpty());

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=?", Integer.class, learner));
    }

    @Test
    void twoLateExaminersOfTheSameJobAreTwoNotices() {
        UUID learner = UUID.randomUUID(), job = UUID.randomUUID();
        for (int i = 0; i < 2; i++) {
            var event = overdue(learner, UUID.randomUUID(), job);
            record.execute(event, event.eventType(), event.eventId().toString());
        }

        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM notifications WHERE grading_job_id=?", Integer.class, job));
    }
}
