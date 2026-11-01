package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.assessment.GradingCompletedTopology;
import com.ieltspath.events.assessment.GradingDueSoonTopology;
import com.ieltspath.events.assessment.GradingOverdueTopology;
import com.ieltspath.events.assessment.GradingRequestedTopology;
import com.ieltspath.events.access.SubscriptionGrantedTopology;
import com.ieltspath.events.community.CommunityMessaging;
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

/** The dead-letter queues Notification consumes, and the queues their messages go back to when an operator replays them. */
@Configuration
public class DeadLetterReplayConfig {

    @Bean
    DeadLetterReplayer notificationDeadLetterReplayer(ConnectionFactory connectionFactory, AmqpAdmin amqpAdmin) {
        return new DeadLetterReplayer(connectionFactory, amqpAdmin, List.of(
                new DeadLetterRoute(GradingCompletedTopology.DLQ, GradingCompletedTopology.QUEUE),
                new DeadLetterRoute(GradingRequestedTopology.DLQ, GradingRequestedTopology.QUEUE),
                new DeadLetterRoute(GradingOverdueTopology.DLQ, GradingOverdueTopology.QUEUE),
                new DeadLetterRoute(GradingDueSoonTopology.DLQ, GradingDueSoonTopology.QUEUE),
                new DeadLetterRoute(SubscriptionGrantedTopology.DLQ, SubscriptionGrantedTopology.QUEUE),
                new DeadLetterRoute(CommunityMessaging.NOTIFICATIONS_DLQ, CommunityMessaging.NOTIFICATIONS_QUEUE)));
    }

    @Bean
    MeterBinder notificationDeadLetterMetrics(DeadLetterReplayer replayer) {
        return new DeadLetterMeterBinder(replayer.routes().stream().map(DeadLetterRoute::deadLetterQueue).toList(),
                () -> replayer.counts().stream().collect(Collectors.toMap(
                        count -> count.deadLetterQueue(), count -> count.messages(), (left, right) -> right)));
    }
}
