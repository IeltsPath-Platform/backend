package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerDirectory;
import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.assessment.GradingRequestedTopology;
import com.ieltspath.events.assessment.GradingRequestedV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The examiner's notice reaches the broker a moment after the learner's request commits, without waiting for the relay's
 * timer: the timer is set to once an hour here, so only the push after the commit can deliver it in time.
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "assessment.outbox.relay.enabled=true",
        "assessment.outbox.relay.poll-interval=PT1H",
        "assessment.examiner.overdue-notice.initial-delay=PT1H", "assessment.examiner.overdue-notice.poll-interval=PT1H",
        "assessment.llm-grading.enabled=false", "assessment.llm-grading.daily-limit=100",
        "spring.rabbitmq.publisher-confirm-type=correlated", "spring.rabbitmq.publisher-returns=true",
        "spring.rabbitmq.template.mandatory=true"})
class GradingRequestedBrokerIntegrationTest extends ExaminerIntegrationSupport {
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
        jdbc.update("DELETE FROM human_review_submissions");
        jdbc.update("DELETE FROM outbox_events WHERE event_type='assessment.grading-requested.v1'");
        when(examinerDirectory.find(examiner)).thenReturn(Optional.of(new ExaminerDirectory.Examiner(examiner, true, 10)));
    }

    @Test
    void theExaminerNoticeReachesTheNotificationQueueRightAfterTheLearnerSends() throws Exception {
        String body = """
                {"requestId":"%s","sourceSubmissionId":"%s","source":"PRACTICE","task":"TASK_2",
                 "promptSnapshot":{"stem":"Discuss both views."},"essay":"An essay.",
                 "rubric":{"code":"IELTS_WRITING_TASK_2","version":1,"criteria":[]},"examinerId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), examiner);
        String answer = mvc.perform(post("/internal/assessments/human-reviews")
                        .header("Authorization", "Bearer " + internalToken(learner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        UUID review = UUID.fromString(json.readTree(answer).get("reviewSubmissionId").asText());
        UUID job = jdbc.queryForObject("SELECT id FROM grading_jobs WHERE human_review_submission_id=?", UUID.class, review);

        Message sent = rabbit.receive(GradingRequestedTopology.QUEUE, 5_000);

        assertNotNull(sent, "the notice was pushed after the commit, not left for the hourly timer");
        var event = json.readValue(new String(sent.getBody(), StandardCharsets.UTF_8), GradingRequestedV1.class);
        assertEquals(event.eventId().toString(), sent.getMessageProperties().getMessageId());
        assertEquals(AssessmentMessaging.GRADING_REQUESTED_ROUTING_KEY, sent.getMessageProperties().getReceivedRoutingKey());
        assertEquals(examiner, event.examinerId());
        assertEquals(job, event.gradingJobId());
    }
}
