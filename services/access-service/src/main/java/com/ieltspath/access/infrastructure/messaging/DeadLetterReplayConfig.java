package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.events.messaging.DeadLetterReplayer;
import com.ieltspath.events.messaging.DeadLetterRoute;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** The dead-letter queue Access consumes from, and the queue its messages go back to when an operator replays them. */
@Configuration
public class DeadLetterReplayConfig {

    @Bean
    DeadLetterReplayer accessDeadLetterReplayer(ConnectionFactory connectionFactory, AmqpAdmin amqpAdmin) {
        return new DeadLetterReplayer(connectionFactory, amqpAdmin, List.of(
                new DeadLetterRoute(LearningPointsMessagingConfig.GRANT_DLQ, LearningPointsMessagingConfig.GRANT_QUEUE)));
    }
}
