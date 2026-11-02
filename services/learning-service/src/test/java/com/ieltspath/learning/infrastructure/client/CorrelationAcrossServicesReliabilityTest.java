package com.ieltspath.learning.infrastructure.client;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.commonobservability.CorrelationIdFilter;
import com.ieltspath.commonobservability.CorrelationListenerAdvice;
import com.ieltspath.events.points.PointsGrantMessaging;
import com.ieltspath.learning.application.service.RewardGrantService;
import com.ieltspath.learning.infrastructure.messaging.RabbitPointsGrantPublisher;
import com.ieltspath.learning.infrastructure.persistence.JdbcRewardGrantRepository;
import com.ieltspath.learning.infrastructure.scheduler.RewardGrantRelayJob;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.http.HttpStatus;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class CorrelationAcrossServicesReliabilityTest {
    private static final String CORRELATION_ID = "http-to-consumer-42";
    private static final UUID USER_ID = UUID.fromString("8b9e2526-4388-4980-b5ea-2c76b14cd67e");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static final GenericContainer<?> RABBIT = new GenericContainer<>(
            DockerImageName.parse("rabbitmq:3.13-management-alpine"))
            .withExposedPorts(5672)
            .withEnv("RABBITMQ_DEFAULT_USER", "test")
            .withEnv("RABBITMQ_DEFAULT_PASS", "test")
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @Test
    void sameCorrelationIdInHttpAndConsumerLogs() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE reward_grants (
                    id UUID PRIMARY KEY,
                    user_id UUID NOT NULL,
                    amount INTEGER NOT NULL,
                    reason VARCHAR(80) NOT NULL,
                    reference_id VARCHAR(160) NOT NULL,
                    correlation_id VARCHAR(100),
                    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
                    attempts INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                    last_error VARCHAR(200),
                    UNIQUE (user_id, reason, reference_id)
                )
                """);

        RestClient.Builder contentBuilder = RestClient.builder();
        MockRestServiceServer content = MockRestServiceServer.bindTo(contentBuilder).build();
        RestLearningContentClient contentClient = new RestLearningContentClient(
                contentBuilder.baseUrl("http://content.test").build());
        RestClient.Builder assessmentBuilder = RestClient.builder();
        MockRestServiceServer assessment = MockRestServiceServer.bindTo(assessmentBuilder).build();
        RestAssessmentStudyContextClient assessmentClient = new RestAssessmentStudyContextClient(
                assessmentBuilder.baseUrl("http://assessment.test").build());

        CachingConnectionFactory connection = new CachingConnectionFactory(
                RABBIT.getHost(), RABBIT.getMappedPort(5672));
        connection.setUsername("test");
        connection.setPassword("test");
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        RabbitTemplate rabbit = new RabbitTemplate(connection);
        rabbit.setMandatory(true);
        RabbitAdmin admin = new RabbitAdmin(connection);
        TopicExchange exchange = new TopicExchange(PointsGrantMessaging.EXCHANGE, true, false);
        Queue queue = new Queue("learning.correlation." + UUID.randomUUID(), true, false, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange)
                .with(PointsGrantMessaging.REQUESTED_ROUTING_KEY));

        JdbcRewardGrantRepository grants = new JdbcRewardGrantRepository(jdbc);
        RewardGrantService rewardService = new RewardGrantService(
                grants, new RabbitPointsGrantPublisher(rabbit, new com.fasterxml.jackson.databind.ObjectMapper(),
                Duration.ofSeconds(5)), 10, Duration.ofMinutes(5), 3);
        RewardGrantRelayJob relay = new RewardGrantRelayJob(rewardService, jdbc,
                new DataSourceTransactionManager(dataSource), true);

        AtomicReference<String> consumerCorrelation = new AtomicReference<>();
        CountDownLatch consumed = new CountDownLatch(1);
        Logger consumerLogger = (Logger) LoggerFactory.getLogger("learning.integration.consumer");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        consumerLogger.addAppender(appender);
        MessageListener consumer = message -> {
            consumerCorrelation.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            consumerLogger.info("Consumed points grant request");
            consumed.countDown();
        };
        ProxyFactory proxy = new ProxyFactory(consumer);
        proxy.setInterfaces(MessageListener.class);
        proxy.addAdvice(new CorrelationListenerAdvice());
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connection);
        container.setQueueNames(queue.getName());
        container.setMessageListener((MessageListener) proxy.getProxy());

        UUID assessmentId = UUID.randomUUID();
        content.expect(requestTo("http://content.test/internal/learning-content/topic-sequence"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer reliability-token"))
                .andExpect(header(Correlation.HEADER, CORRELATION_ID))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assessment.expect(requestTo(org.hamcrest.Matchers.containsString(
                        "http://assessment.test/api/assessments/me/statistics/study-context?sourceType=ASSESSMENT&sourceId="
                                + assessmentId)))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer reliability-token"))
                .andExpect(header(Correlation.HEADER, CORRELATION_ID))
                .andRespond(withSuccess("{\"sourceId\":\"" + assessmentId
                        + "\",\"courseId\":\"" + UUID.randomUUID() + "\"}", MediaType.APPLICATION_JSON));

        Jwt jwt = Jwt.withTokenValue("reliability-token").header("alg", "none")
                .subject(USER_ID.toString()).build();
        MockMvc http = MockMvcBuilders.standaloneSetup(
                        new CorrelationWorkflowEndpoint(contentClient, assessmentClient, grants))
                .addFilters(new CorrelationIdFilter(), (request, response, chain) -> {
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
                    authentication.setAuthenticated(true);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    try {
                        chain.doFilter(request, response);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                })
                .build();
        try {
            container.start();
            http.perform(post("/test/reliability/{assessmentId}", assessmentId)
                            .header(Correlation.HEADER, CORRELATION_ID))
                    .andExpect(status().isNoContent());
            content.verify();
            assessment.verify();

            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM reward_grants", Integer.class));
            assertEquals(CORRELATION_ID, jdbc.queryForObject(
                    "SELECT correlation_id FROM reward_grants", String.class));
            assertEquals(1, relay.runOnce());
            assertTrue(consumed.await(5, TimeUnit.SECONDS), "RabbitMQ consumer should receive the outbox event");
            assertEquals(CORRELATION_ID, consumerCorrelation.get());
            assertTrue(appender.list.stream().anyMatch(event ->
                    CORRELATION_ID.equals(event.getMDCPropertyMap().get(CorrelationIdFilter.MDC_KEY))),
                    "consumer log should retain the correlation id from the originating HTTP request");
            assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        } finally {
            SecurityContextHolder.clearContext();
            container.stop();
            consumerLogger.detachAppender(appender);
            appender.stop();
            admin.deleteQueue(queue.getName());
            connection.destroy();
        }
    }

    @RestController
    private static class CorrelationWorkflowEndpoint {
        private final RestLearningContentClient content;
        private final RestAssessmentStudyContextClient assessment;
        private final JdbcRewardGrantRepository grants;

        private CorrelationWorkflowEndpoint(RestLearningContentClient content,
                                            RestAssessmentStudyContextClient assessment,
                                            JdbcRewardGrantRepository grants) {
            this.content = content;
            this.assessment = assessment;
            this.grants = grants;
        }

        @PostMapping("/test/reliability/{assessmentId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void createEvent(@PathVariable UUID assessmentId) {
            content.getTopicSequence();
            assessment.context(assessmentId);
            grants.enqueue(USER_ID, "CORRELATION_CHECK", assessmentId.toString(), 1);
        }
    }
}
