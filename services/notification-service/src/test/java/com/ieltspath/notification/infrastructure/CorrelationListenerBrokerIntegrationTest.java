package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.community.CommunityEvent;
import com.ieltspath.events.community.CommunityEventTypes;
import com.ieltspath.events.community.CommunityMessaging;
import com.ieltspath.notification.application.RecordCommunityEventUseCase;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "spring.rabbitmq.publisher-confirm-type=correlated", "spring.rabbitmq.publisher-returns=true",
        "spring.rabbitmq.template.mandatory=true",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="
})
class CorrelationListenerBrokerIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container static final GenericContainer<?> RABBIT = new GenericContainer<>("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672).waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    }

    @Autowired RabbitTemplate rabbit;
    @Autowired ObjectMapper mapper;
    @MockitoSpyBean RecordCommunityEventUseCase recordEvent;

    @Test
    void listenerSeesTheCorrelationIdOfTheMessageAndClearsIt() throws Exception {
        var seen = new CopyOnWriteArrayList<String>();
        var handled = new CountDownLatch(2);
        doAnswer(invocation -> {
            seen.add(MDC.get("correlationId"));
            handled.countDown();
            return Optional.empty();
        }).when(recordEvent).execute(any(CommunityEvent.class), anyString(), anyString());

        publish("req-notification-1", "first");
        publish(null, "second");

        assertTrue(handled.await(10, TimeUnit.SECONDS), "both messages were consumed");
        assertEquals("req-notification-1", seen.get(0));
        assertTrue(seen.get(1) != null && !seen.get(1).isBlank(), "a message without a correlation id gets a fresh one");
    }

    private void publish(String correlationId, String suffix) throws JsonProcessingException {
        UUID id = UUID.nameUUIDFromBytes(suffix.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var event = new CommunityEvent(id, CommunityEventTypes.USER_FOLLOWED, Instant.now(), UUID.randomUUID(),
                UUID.randomUUID(), null, null);
        MessagePostProcessor headers = message -> {
            message.getMessageProperties().setMessageId(id.toString());
            if (correlationId != null) message.getMessageProperties().setHeader("x-correlation-id", correlationId);
            return message;
        };
        rabbit.convertAndSend(CommunityMessaging.EXCHANGE, CommunityEventTypes.USER_FOLLOWED,
                mapper.writeValueAsBytes(event), headers);
    }
}
