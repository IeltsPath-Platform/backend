package com.ieltspath.community.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.community.CommunityMessaging;
import com.ieltspath.events.community.CommunityEventTypes;
import com.ieltspath.community.infrastructure.messaging.CommunityEventOutboxAdapter;
import com.ieltspath.community.infrastructure.messaging.CommunityOutboxRelay;
import com.ieltspath.commonobservability.CorrelationIdFilter;
import com.ieltspath.commonobservability.CorrelationListenerAdvice;
import com.ieltspath.commonobservability.OutboxTraceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@AutoConfigureObservability
@SpringBootTest(classes = CommunityOutboxRelayIntegrationTest.Application.class, properties = {
        "management.tracing.enabled=true", "management.tracing.sampling.probability=1.0",
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="
})
class CommunityOutboxRelayIntegrationTest {
    @Configuration
    @EnableAutoConfiguration
    static class Application { }

    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container static final GenericContainer<?> RABBIT = new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
            .withExposedPorts(5672).withEnv("RABBITMQ_DEFAULT_USER","test").withEnv("RABBITMQ_DEFAULT_PASS","test")
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*",1));

    private static JdbcTemplate outboxTable() {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()));
        jdbc.execute("DROP TABLE IF EXISTS outbox_events");
        jdbc.execute("CREATE TABLE outbox_events(id UUID PRIMARY KEY,aggregate_type VARCHAR(100) NOT NULL,aggregate_id VARCHAR(255) NOT NULL,event_type VARCHAR(100) NOT NULL,payload JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL,published_at TIMESTAMPTZ,next_attempt_at TIMESTAMPTZ,retry_count INTEGER NOT NULL DEFAULT 0,last_error TEXT,failed_at TIMESTAMPTZ,correlation_id VARCHAR(100),trace_context VARCHAR)");
        return jdbc;
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "test");
        registry.add("spring.rabbitmq.password", () -> "test");
    }

    @Autowired Tracer tracer;
    @Autowired OutboxTraceContext outboxTraceContext;
    @Autowired ApplicationContext applicationContext;

    @Test void unsupportedEventIsMarkedFailedOnceInsteadOfRetriedForever() {
        var jdbc=outboxTable();
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at) VALUES (?,'COMMUNITY','x','community.unknown.v1','{}'::jsonb,now())",id);
        var connection=new CachingConnectionFactory(RABBIT.getHost(),RABBIT.getMappedPort(5672));
        connection.setUsername("test"); connection.setPassword("test");
        var relay=new CommunityOutboxRelay(jdbc,new RabbitTemplate(connection),new ObjectMapper(),"community.events",20,Duration.ofSeconds(5),true,12,Duration.ofSeconds(60));
        assertEquals(0,relay.publishPendingBatch());
        assertNotNull(jdbc.queryForObject("SELECT failed_at FROM outbox_events WHERE id=?",java.sql.Timestamp.class,id));
        assertEquals(0,relay.publishPendingBatch());
        connection.destroy();
    }

    @Test void repeatedReactionOrFollowByTheSameActorIsRecordedOnce() {
        var jdbc=outboxTable();
        var outbox=new CommunityEventOutboxAdapter(jdbc,new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()));
        UUID actor=UUID.randomUUID(), recipient=UUID.randomUUID(), post=UUID.randomUUID();
        for(int i=0;i<3;i++) outbox.record(CommunityEventTypes.POST_REACTED,actor,recipient,post,null);
        for(int i=0;i<3;i++) outbox.record(CommunityEventTypes.USER_FOLLOWED,actor,recipient,null,null);
        outbox.record(CommunityEventTypes.POST_REACTED,UUID.randomUUID(),recipient,post,null);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE event_type=?",Integer.class,CommunityEventTypes.POST_REACTED));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE event_type=?",Integer.class,CommunityEventTypes.USER_FOLLOWED));
    }

    @Test void everyAcceptedOutboxTypeRoutesThroughRealRabbitMq() {
        var source=new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        var jdbc=outboxTable();

        var connection=new CachingConnectionFactory(RABBIT.getHost(),RABBIT.getMappedPort(5672));
        connection.setUsername("test"); connection.setPassword("test");
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        var rabbit=new RabbitTemplate(connection); rabbit.setMandatory(true);
        var admin=new RabbitAdmin(connection);
        var exchange=new TopicExchange("community.events",true,false);
        var queue=new Queue("community.events.routing-test",true,false,false);
        admin.declareExchange(exchange); admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("community.#"));

        var mapper=new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        var outbox=new CommunityEventOutboxAdapter(jdbc,mapper);
        UUID actor=UUID.randomUUID(), recipient=UUID.randomUUID(), post=UUID.randomUUID(), comment=UUID.randomUUID();
        for(String type:CommunityEventTypes.ALL) outbox.record(type,actor,recipient,
                CommunityEventTypes.USER_FOLLOWED.equals(type)?null:post,
                type.contains("commented")||type.contains("replied")||type.contains("mentioned")?comment:null);
        var relay=new CommunityOutboxRelay(jdbc,rabbit,mapper,"community.events",20,Duration.ofSeconds(5),true,12,Duration.ofSeconds(60));

        assertEquals(CommunityEventTypes.ALL.size(),relay.publishPendingBatch());
        Set<String> received=new HashSet<>();
        for(int i=0;i<CommunityEventTypes.ALL.size();i++) {
            var message=rabbit.receive(queue.getName(),5000);
            assertNotNull(message,"Expected event " + i + " to reach the bound queue");
            received.add(message.getMessageProperties().getReceivedRoutingKey());
        }
        assertEquals(CommunityEventTypes.ALL,received);
        connection.destroy();
    }

    @Test void sendsHeaderWhenRowHasCorrelationId() {
        assertCorrelationHeader("req-community", "req-community");
    }

    @Test
    void correlationSurvivesHttpOutboxRelayAndBrokerConsumerLogs() throws Exception {
        var jdbc = outboxTable();
        var connection = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getMappedPort(5672));
        connection.setUsername("test");
        connection.setPassword("test");
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        var rabbit = new RabbitTemplate(connection);
        rabbit.setMandatory(true);
        var admin = new RabbitAdmin(connection);
        var exchange = new TopicExchange(CommunityMessaging.EXCHANGE, true, false);
        var queue = new Queue("community.events.http-correlation." + UUID.randomUUID(), true, false, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("community.#"));

        var outbox = new CommunityEventOutboxAdapter(jdbc,
                new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()));
        MockMvc http = MockMvcBuilders.standaloneSetup(new CorrelationProducingEndpoint(outbox))
                .addFilters(new CorrelationIdFilter()).build();
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/test/events")
                        .header("X-Correlation-Id", "http-request-42"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());

        assertEquals("http-request-42", jdbc.queryForObject(
                "SELECT correlation_id FROM outbox_events ORDER BY created_at DESC LIMIT 1", String.class));
        var seenByConsumer = new AtomicReference<String>();
        var handled = new CountDownLatch(1);
        Logger consumerLogger = (Logger) LoggerFactory.getLogger("correlation.integration.consumer");
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        consumerLogger.addAppender(appender);
        MessageListener listener = message -> {
            seenByConsumer.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            consumerLogger.info("Consumed community event");
            handled.countDown();
        };
        var proxy = new ProxyFactory(listener);
        proxy.addAdvice(new CorrelationListenerAdvice());
        var container = new SimpleMessageListenerContainer(connection);
        container.setQueueNames(queue.getName());
        container.setMessageListener((MessageListener) proxy.getProxy());
        try {
            container.start();
            assertEquals("http-request-42", jdbc.queryForObject(
                    "SELECT correlation_id FROM outbox_events ORDER BY created_at DESC LIMIT 1", String.class));
            var relay = new CommunityOutboxRelay(jdbc, rabbit,
                    new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()),
                    CommunityMessaging.EXCHANGE, 20, Duration.ofSeconds(5), true, 12, Duration.ofSeconds(60));
            assertEquals(1, relay.publishPendingBatch());

            assertTrue(handled.await(5, TimeUnit.SECONDS), "broker listener should consume the relayed event");
            assertEquals("http-request-42", seenByConsumer.get());
            assertTrue(appender.list.stream().anyMatch(event ->
                    "http-request-42".equals(event.getMDCPropertyMap().get(CorrelationIdFilter.MDC_KEY))),
                    "consumer log should carry the originating request correlation id");
            assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        } finally {
            container.stop();
            consumerLogger.detachAppender(appender);
            appender.stop();
            admin.deleteQueue(queue.getName());
            connection.destroy();
        }
    }

    @Test void omitsHeaderWhenRowHasNone() {
        assertCorrelationHeader(null, null);
    }

    @Test
    void legacyOutboxRowWithoutTraceContextStillReachesRabbitMq() {
        var jdbc = outboxTable();
        var connection = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getMappedPort(5672));
        connection.setUsername("test");
        connection.setPassword("test");
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        var rabbit = new RabbitTemplate(connection);
        rabbit.setMandatory(true);
        var admin = new RabbitAdmin(connection);
        var exchange = new TopicExchange(CommunityMessaging.EXCHANGE, true, false);
        var queue = new Queue("community.events.legacy." + UUID.randomUUID(), true, false, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("community.#"));

        jdbc.update("""
                INSERT INTO outbox_events(id, aggregate_type, aggregate_id, event_type, payload, created_at,
                                          correlation_id, trace_context)
                VALUES (?, 'COMMUNITY', 'legacy', ?, '{}'::jsonb, now(), 'old-correlation', NULL)
                """, UUID.randomUUID(), CommunityEventTypes.POST_REACTED);
        var relay = new CommunityOutboxRelay(jdbc, rabbit, new ObjectMapper(), CommunityMessaging.EXCHANGE,
                20, Duration.ofSeconds(5), true, 12, Duration.ofSeconds(60), outboxTraceContext);

        assertEquals(1, relay.publishPendingBatch());
        assertNotNull(rabbit.receive(queue.getName(), 5_000));

        admin.deleteQueue(queue.getName());
        connection.destroy();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void requestTraceSurvivesScheduledOutboxRelayRabbitAndConsumer(boolean failOnce) throws Exception {
        var jdbc = outboxTable();
        var connection = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getMappedPort(5672));
        connection.setUsername("test");
        connection.setPassword("test");
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        var rabbit = new RabbitTemplate(connection);
        rabbit.setMandatory(true);
        rabbit.setObservationEnabled(true);
        rabbit.setApplicationContext(applicationContext);
        var admin = new RabbitAdmin(connection);
        var exchange = new TopicExchange(CommunityMessaging.EXCHANGE, true, false);
        var queue = new Queue("community.events.trace." + UUID.randomUUID(), true, false, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("community.#"));

        var mapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        var outbox = new CommunityEventOutboxAdapter(jdbc, mapper, outboxTraceContext);
        MockMvc http = MockMvcBuilders.standaloneSetup(new CorrelationProducingEndpoint(outbox))
                .addFilters(new CorrelationIdFilter()).build();
        Span request = tracer.nextSpan().name("http.request").start();
        String traceId = request.context().traceId();
        try (Tracer.SpanInScope ignored = tracer.withSpan(request)) {
            http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/test/events")
                            .header("X-Correlation-Id", "trace-request-42"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
        } finally {
            request.end();
        }
        String storedTraceContext = jdbc.queryForObject(
                "SELECT trace_context FROM outbox_events ORDER BY created_at DESC LIMIT 1", String.class);
        assertNotNull(storedTraceContext);
        assertEquals(traceId, storedTraceContext.split("-", 3)[1]);

        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<String> consumedTraceId = new AtomicReference<>();
        AtomicReference<String> consumedCorrelationId = new AtomicReference<>();
        var handled = new CountDownLatch(1);
        var container = new SimpleMessageListenerContainer(connection);
        container.setQueueNames(queue.getName());
        container.setObservationEnabled(true);
        container.setApplicationContext(applicationContext);
        container.setDefaultRequeueRejected(true);
        container.setMessageListener((MessageListener) message -> {
            int delivery = attempts.incrementAndGet();
            if (failOnce && delivery == 1) throw new IllegalStateException("retry once");
            consumedTraceId.set(tracer.currentSpan().context().traceId());
            consumedCorrelationId.set(message.getMessageProperties().getHeader("x-correlation-id"));
            handled.countDown();
        });
        try {
            container.start();
            var relay = new CommunityOutboxRelay(jdbc, rabbit, mapper, CommunityMessaging.EXCHANGE, 20,
                    Duration.ofSeconds(5), true, 12, Duration.ofSeconds(60), outboxTraceContext);
            var scheduler = new com.ieltspath.community.infrastructure.messaging.CommunityOutboxRelayScheduler(relay);
            java.util.concurrent.CompletableFuture.runAsync(scheduler::publishPending);

            assertTrue(handled.await(10, TimeUnit.SECONDS), "broker consumer should succeed after one retry");
            assertEquals(failOnce ? 2 : 1, attempts.get());
            assertEquals(traceId, consumedTraceId.get());
            assertEquals("trace-request-42", consumedCorrelationId.get());
        } finally {
            container.stop();
            admin.deleteQueue(queue.getName());
            connection.destroy();
        }
    }

    private void assertCorrelationHeader(String storedCorrelation, String expectedHeader) {
        var jdbc = outboxTable();
        var connection = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getMappedPort(5672));
        connection.setUsername("test");
        connection.setPassword("test");
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        var rabbit = new RabbitTemplate(connection);
        rabbit.setMandatory(true);
        var admin = new RabbitAdmin(connection);
        var exchange = new TopicExchange(CommunityMessaging.EXCHANGE, true, false);
        var queue = new Queue("community.events.correlation." + UUID.randomUUID(), true, false, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("community.#"));

        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO outbox_events(id, aggregate_type, aggregate_id, event_type, payload, created_at, correlation_id)
                VALUES (?, 'COMMUNITY', 'aggregate', ?, '{}'::jsonb, now(), ?)
                """, id, CommunityEventTypes.POST_REACTED, storedCorrelation);
        var relay = new CommunityOutboxRelay(jdbc, rabbit,
                new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()),
                CommunityMessaging.EXCHANGE, 20, Duration.ofSeconds(5), true, 12, Duration.ofSeconds(60));

        assertEquals(1, relay.publishPendingBatch());
        var message = rabbit.receive(queue.getName(), 5_000);
        assertNotNull(message);
        assertEquals(expectedHeader, message.getMessageProperties().getHeader("x-correlation-id"));
        admin.deleteQueue(queue.getName());
        connection.destroy();
    }

    @RestController
    private static class CorrelationProducingEndpoint {
        private final CommunityEventOutboxAdapter outbox;

        private CorrelationProducingEndpoint(CommunityEventOutboxAdapter outbox) {
            this.outbox = outbox;
        }

        @PostMapping("/test/events")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void createEvent() {
            outbox.record(CommunityEventTypes.POST_COMMENTED, UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), null);
        }
    }
}
