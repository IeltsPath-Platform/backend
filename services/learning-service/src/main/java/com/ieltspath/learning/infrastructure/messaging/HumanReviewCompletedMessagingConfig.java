package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.assessment.HumanReviewCompletedTopology;
import com.ieltspath.events.messaging.MessageRetry;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology of the queue that carries an examiner's finished Writing review from Assessment to Learning. A refused message is
 * dead-lettered; a failure that may pass is parked on the retry queue and returns after the retry delay, up to the allowed
 * number of retries.
 */
@Configuration
public class HumanReviewCompletedMessagingConfig {
    public static final String QUEUE = HumanReviewCompletedTopology.QUEUE;
    public static final String CONTAINER_FACTORY = "humanReviewCompletedListenerContainerFactory";
    public static final String RETRY = "humanReviewCompletedRetry";

    /** Built by the shared helper, so it matches the declaration Assessment makes of the same queue. */
    @Bean
    Declarables humanReviewCompletedTopology() {
        return HumanReviewCompletedTopology.declarables(AssessmentMessaging.EXCHANGE);
    }

    /** A refused message is dead-lettered, never put back on the queue where it would be redelivered at once. */
    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory humanReviewCompletedListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry humanReviewCompletedRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, HumanReviewCompletedTopology.RETRY_QUEUE,
                HumanReviewCompletedTopology.MAX_RETRIES);
    }
}
