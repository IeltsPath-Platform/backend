package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.access.AccessMessaging;
import com.ieltspath.events.access.SubscriptionGrantedTopology;
import com.ieltspath.events.messaging.MessageRetry;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology of the queue that carries "your Premium subscription was granted" notices from Access. A message the consumer
 * refuses is dead-lettered to {@link #DLQ}; a failure that may pass is parked on {@link #RETRY_QUEUE} and returns to
 * {@link #QUEUE} after {@link #RETRY_DELAY_MS}.
 */
@Configuration
public class SubscriptionGrantedMessagingConfig {
    public static final String QUEUE = SubscriptionGrantedTopology.QUEUE;
    public static final String RETRY_QUEUE = SubscriptionGrantedTopology.RETRY_QUEUE;
    public static final String DLQ = SubscriptionGrantedTopology.DLQ;
    public static final String CONTAINER_FACTORY = "subscriptionGrantedListenerContainerFactory";
    public static final String RETRY = "subscriptionGrantedRetry";
    public static final long RETRY_DELAY_MS = SubscriptionGrantedTopology.RETRY_DELAY_MS;
    public static final int MAX_RETRIES = SubscriptionGrantedTopology.MAX_RETRIES;

    /** Built by the shared helper, so it is identical to the declaration Access makes of the same queue. */
    @Bean
    Declarables subscriptionGrantedNoticeTopology() {
        return SubscriptionGrantedTopology.declarables(AccessMessaging.EXCHANGE);
    }

    /** A refused message is dead-lettered, never put back on the queue where it would be redelivered at once. */
    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory subscriptionGrantedListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry subscriptionGrantedRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, RETRY_QUEUE, MAX_RETRIES);
    }
}
