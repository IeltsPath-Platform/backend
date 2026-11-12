package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.messaging.MessageRetry;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubscriptionExpiryInboxMessagingConfig {
    public static final String CONTAINER_FACTORY = "subscriptionExpiryInboxListenerContainerFactory";
    public static final String RETRY = "subscriptionExpiryInboxRetry";

    @Bean Declarables subscriptionExpiryInboxTopology() { return SubscriptionExpiryInboxTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory subscriptionExpiryInboxListenerContainerFactory(ConnectionFactory connectionFactory) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry subscriptionExpiryInboxRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, SubscriptionExpiryInboxTopology.RETRY_QUEUE,
                SubscriptionExpiryInboxTopology.MAX_RETRIES);
    }
}
