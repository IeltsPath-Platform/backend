package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.events.messaging.DeadLetterReplayer;
import com.ieltspath.events.messaging.DeadLetterRoute;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** The dead-letter queues Learning consumes from, and the queues their messages go back to when an operator replays them. */
@Configuration
public class DeadLetterReplayConfig {

    @Bean
    DeadLetterReplayer learningDeadLetterReplayer(ConnectionFactory connectionFactory, AmqpAdmin amqpAdmin) {
        return new DeadLetterReplayer(connectionFactory, amqpAdmin, List.of(
                new DeadLetterRoute(RewardGrantMessagingConfig.SETTLED_DLQ, RewardGrantMessagingConfig.SETTLED_QUEUE),
                new DeadLetterRoute(AssessmentMessagingConfig.DEAD_LETTER_QUEUE, AssessmentMessagingConfig.QUEUE)));
    }
}
