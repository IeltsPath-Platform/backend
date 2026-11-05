package com.ieltspath.assessment.application.usecase;

import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.assessment.GradingOverdueTopology;
import com.ieltspath.events.assessment.GradingOverdueV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * The late-examiner notice from the timer to the broker, with nothing replaced by a mock: the scheduler finds the late job
 * by itself, the outbox relay publishes it to a real RabbitMQ, and it reaches the queue Notification reads from. No
 * Notification service runs here, so the queue exists only because Assessment declared it.
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "assessment.outbox.relay.enabled=true",
        "assessment.outbox.relay.poll-interval=PT0.5S",
        "assessment.examiner.overdue-notice.initial-delay=PT0S", "assessment.examiner.overdue-notice.poll-interval=PT1S",
        "assessment.llm-grading.enabled=false", "assessment.llm-grading.daily-limit=100",
        "spring.rabbitmq.publisher-confirm-type=correlated", "spring.rabbitmq.publisher-returns=true",
        "spring.rabbitmq.template.mandatory=true"})
class GradingOverdueBrokerIntegrationTest extends ExaminerIntegrationSupport {
    @Container
    static final GenericContainer<?> RABBITMQ = new GenericContainer<>("rabbitmq:3.13-alpine")
            .withExposedPorts(5672).waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @DynamicPropertySource
    static void rabbit(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Autowired RabbitTemplate rabbit;

    @BeforeEach
    void freshJobs() {
        jdbc.update("DELETE FROM human_reviews");
        jdbc.update("DELETE FROM grading_jobs");
        jdbc.update("DELETE FROM outbox_events WHERE event_type='assessment.grading-overdue.v1'");
        when(examinerDirectory.find(examiner)).thenReturn(java.util.Optional.of(
                new com.ieltspath.assessment.application.port.ExaminerDirectory.Examiner(examiner, true, 10)));
    }

    @Test
    void aLateJobIsFoundByTheTimerAndReachesTheNotificationQueueOnce() throws Exception {
        UUID job = humanJob(courseEssay());
        // Not late yet: the timer ticks several times and nothing is published.
        Thread.sleep(3000);
        assertNull(rabbit.receive(GradingOverdueTopology.QUEUE), "nothing is sent before the deadline");

        jdbc.update("UPDATE grading_jobs SET requested_at=CURRENT_TIMESTAMP-INTERVAL '49 hours' WHERE id=?", job);

        Message sent = rabbit.receive(GradingOverdueTopology.QUEUE, 20_000);
        assertNotNull(sent, "the notice reached the queue without any help from Notification");
        var event = json.readValue(new String(sent.getBody(), StandardCharsets.UTF_8), GradingOverdueV1.class);
        assertEquals(GradingOverdueV1.EVENT_TYPE, event.eventType());
        assertEquals(event.eventId().toString(), sent.getMessageProperties().getMessageId());
        assertEquals(AssessmentMessaging.GRADING_OVERDUE_ROUTING_KEY, sent.getMessageProperties().getReceivedRoutingKey());
        assertEquals(learner, event.learnerId());
        assertEquals(examiner, event.examinerId());
        assertEquals(job, event.gradingJobId());
        assertEquals("WRITING", event.skill());

        // The relay marked the outbox row as published, and later ticks send nothing more.
        // (the relay marks the row after the broker confirmed, which can be a moment after the message is readable)
        for (int i = 0; i < 50 && count("SELECT count(*) FROM outbox_events WHERE event_type='assessment.grading-overdue.v1' AND published_at IS NOT NULL") == 0; i++) {
            Thread.sleep(100);
        }
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type='assessment.grading-overdue.v1' AND published_at IS NOT NULL"));
        Thread.sleep(3000);
        assertNull(rabbit.receive(GradingOverdueTopology.QUEUE), "the learner is told once");
    }
}
