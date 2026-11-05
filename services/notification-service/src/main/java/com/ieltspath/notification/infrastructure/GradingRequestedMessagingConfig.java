package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.assessment.GradingRequestedTopology;
import com.ieltspath.events.messaging.MessageRetry;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology of the queue that carries "a learner chose you" notices from Assessment. A message the consumer refuses is
 * dead-lettered to {@link #DLQ}; a failure that may pass is parked on {@link #RETRY_QUEUE} and returns to {@link #QUEUE}
 * after {@link #RETRY_DELAY_MS}.
 */
@Configuration
public class GradingRequestedMessagingConfig {
    public static final String QUEUE = GradingRequestedTopology.QUEUE;
    public static final String RETRY_QUEUE = GradingRequestedTopology.RETRY_QUEUE;
    public static final String DLQ = GradingRequestedTopology.DLQ;
    public static final String CONTAINER_FACTORY = "gradingRequestedListenerContainerFactory";
    public static final String RETRY = "gradingRequestedRetry";
    public static final long RETRY_DELAY_MS = GradingRequestedTopology.RETRY_DELAY_MS;
    public static final int MAX_RETRIES = GradingRequestedTopology.MAX_RETRIES;

    /** Built by the shared helper, so it is identical to the declaration Assessment makes of the same queue. */
    @Bean
    Declarables gradingRequestedTopology() {
        return GradingRequestedTopology.declarables(AssessmentMessaging.EXCHANGE);
    }

    /** A refused message is dead-lettered, never put back on the queue where it would be redelivered at once. */
    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory gradingRequestedListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry gradingRequestedRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, RETRY_QUEUE, MAX_RETRIES);
    }
}
