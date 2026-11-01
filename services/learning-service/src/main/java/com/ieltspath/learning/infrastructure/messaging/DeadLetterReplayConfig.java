package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.events.messaging.DeadLetterReplayer;
import com.ieltspath.events.messaging.DeadLetterRoute;
import com.ieltspath.commonobservability.DeadLetterMeterBinder;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/** The dead-letter queues Learning consumes from, and the queues their messages go back to when an operator replays them. */
@Configuration
public class DeadLetterReplayConfig {

    @Bean
    DeadLetterReplayer learningDeadLetterReplayer(ConnectionFactory connectionFactory, AmqpAdmin amqpAdmin) {
        return new DeadLetterReplayer(connectionFactory, amqpAdmin, List.of(
                new DeadLetterRoute(RewardGrantMessagingConfig.SETTLED_DLQ, RewardGrantMessagingConfig.SETTLED_QUEUE),
                new DeadLetterRoute(AssessmentMessagingConfig.DEAD_LETTER_QUEUE, AssessmentMessagingConfig.QUEUE)));
    }

    @Bean
    MeterBinder learningDeadLetterMetrics(DeadLetterReplayer replayer) {
        return new DeadLetterMeterBinder(replayer.routes().stream().map(DeadLetterRoute::deadLetterQueue).toList(),
                () -> replayer.counts().stream().collect(Collectors.toMap(
                        count -> count.deadLetterQueue(), count -> count.messages(), (left, right) -> right)));
    }
}
