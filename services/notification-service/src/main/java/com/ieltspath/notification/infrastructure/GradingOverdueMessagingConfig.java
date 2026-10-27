package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.assessment.GradingOverdueTopology;
import com.ieltspath.events.messaging.MessageRetry;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology of the queue that carries "the examiner is late" notices from Assessment. A message the consumer refuses is
 * dead-lettered to {@link #DLQ}; a failure that may pass is parked on {@link #RETRY_QUEUE} and returns to {@link #QUEUE}
 * after {@link #RETRY_DELAY_MS}.
 */
@Configuration
public class GradingOverdueMessagingConfig {
    public static final String QUEUE = GradingOverdueTopology.QUEUE;
    public static final String RETRY_QUEUE = GradingOverdueTopology.RETRY_QUEUE;
    public static final String DLQ = GradingOverdueTopology.DLQ;
    public static final String CONTAINER_FACTORY = "gradingOverdueListenerContainerFactory";
    public static final long RETRY_DELAY_MS = GradingOverdueTopology.RETRY_DELAY_MS;
    public static final int MAX_RETRIES = GradingOverdueTopology.MAX_RETRIES;

    /** Built by the shared helper, so it is identical to the declaration Assessment makes of the same queue. */
    @Bean
    Declarables gradingOverdueTopology() {
        return GradingOverdueTopology.declarables(AssessmentMessaging.EXCHANGE);
    }

    /** A refused message is dead-lettered, never put back on the queue where it would be redelivered at once. */
    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory gradingOverdueListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    MessageRetry gradingOverdueRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, RETRY_QUEUE, MAX_RETRIES);
    }
}
