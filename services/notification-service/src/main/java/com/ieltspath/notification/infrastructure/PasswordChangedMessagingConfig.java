package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.PasswordChangedTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordChangedMessagingConfig {
    public static final String CONTAINER_FACTORY = "passwordChangedListenerContainerFactory";
    public static final String RETRY = "passwordChangedRetry";

    @Bean Declarables passwordChangedTopology() { return PasswordChangedTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory passwordChangedListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry passwordChangedRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, PasswordChangedTopology.RETRY_QUEUE, PasswordChangedTopology.MAX_RETRIES);
    }
}
