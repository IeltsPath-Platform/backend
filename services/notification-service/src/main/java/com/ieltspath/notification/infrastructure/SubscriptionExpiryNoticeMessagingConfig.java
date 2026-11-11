package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.access.SubscriptionExpiryNoticeTopology;
import com.ieltspath.events.messaging.MessageRetry;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubscriptionExpiryNoticeMessagingConfig {
    public static final String CONTAINER_FACTORY = "subscriptionExpiryNoticeListenerContainerFactory";
    public static final String RETRY = "subscriptionExpiryNoticeRetry";

    @Bean Declarables subscriptionExpiryNoticeTopology() { return SubscriptionExpiryNoticeTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory subscriptionExpiryNoticeListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry subscriptionExpiryNoticeRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, SubscriptionExpiryNoticeTopology.RETRY_QUEUE,
                SubscriptionExpiryNoticeTopology.MAX_RETRIES);
    }
}
