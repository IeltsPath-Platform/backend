package com.ieltspath.community.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.community.application.port.CommunityEventTypes;
import com.ieltspath.community.infrastructure.messaging.CommunityEventOutboxAdapter;
import com.ieltspath.community.infrastructure.messaging.CommunityOutboxRelay;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
class CommunityOutboxRelayIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container static final GenericContainer<?> RABBIT = new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
            .withExposedPorts(5672).withEnv("RABBITMQ_DEFAULT_USER","test").withEnv("RABBITMQ_DEFAULT_PASS","test")
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*",1));

    @Test void everyAcceptedOutboxTypeRoutesThroughRealRabbitMq() {
        var source=new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        var jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE outbox_events(id UUID PRIMARY KEY,aggregate_type VARCHAR(100) NOT NULL,aggregate_id VARCHAR(255) NOT NULL,event_type VARCHAR(100) NOT NULL,payload JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL,published_at TIMESTAMPTZ,next_attempt_at TIMESTAMPTZ,retry_count INTEGER NOT NULL DEFAULT 0,last_error TEXT)");

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
        var relay=new CommunityOutboxRelay(jdbc,rabbit,mapper,"community.events",20,Duration.ofSeconds(5),true);

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
}
