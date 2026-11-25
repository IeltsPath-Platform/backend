package com.ieltspath.learning.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.events.assessment.SpeakingGradingRequestedV1;
import com.ieltspath.learning.infrastructure.messaging.PlacementSpeakingRequestedListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.rabbitmq.client.Channel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Testcontainers(disabledWithoutDocker = true)
class PlacementSpeakingConsumerIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static PlacementSpeakingRequestedListener listener;
    static JdbcPlacementSpeakingJobStore jobs;
    static ObjectMapper json;

    @BeforeAll static void setup() {
        var source=new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(source).load().migrate();
        jdbc=new JdbcTemplate(source); json=new ObjectMapper().registerModule(new JavaTimeModule());
        var trace=mock(OutboxTraceContext.class);
        jobs=new JdbcPlacementSpeakingJobStore(jdbc);
        listener=new PlacementSpeakingRequestedListener(json,jobs,trace,5);
    }

    @Test void repeatedDeliveryCreatesOnePlacementJobAndAcknowledgesBoth() throws Exception {
        UUID attempt=UUID.randomUUID(),user=UUID.randomUUID();
        var event=new SpeakingGradingRequestedV1(UUID.randomUUID(),SpeakingGradingRequestedV1.EVENT_TYPE,Instant.now(),
                SpeakingGradingRequestedV1.SOURCE,new SpeakingGradingRequestedV1.Data(attempt,user,List.of(
                new SpeakingGradingRequestedV1.Item(UUID.randomUUID(),1,1,"Question","speaking/"+user+"/one.wav"))));
        byte[] body=json.writeValueAsBytes(event);
        Channel channel=mock(Channel.class);
        listener.onMessage(message(body,1),channel);
        listener.onMessage(message(body,2),channel);

        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM placement_speaking_jobs WHERE attempt_id=?",Integer.class,attempt));
        assertEquals(1,jobs.claimDue(5,3,Instant.now(),Instant.now().minusSeconds(300)).size());
        verify(channel).basicAck(1,false); verify(channel).basicAck(2,false);
    }

    private static Message message(byte[] body,long tag) {
        MessageProperties properties=new MessageProperties(); properties.setDeliveryTag(tag);
        properties.setHeader("x-correlation-id","placement-test");
        return new Message(body,properties);
    }
}
