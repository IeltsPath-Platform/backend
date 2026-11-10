package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.EmailVerificationRequestedTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailVerificationRequestedMessagingConfig {
    public static final String CONTAINER_FACTORY = "emailVerificationRequestedListenerContainerFactory";
    public static final String RETRY = "emailVerificationRequestedRetry";

    @Bean Declarables emailVerificationRequestedTopology() { return EmailVerificationRequestedTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory emailVerificationRequestedListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry emailVerificationRequestedRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, EmailVerificationRequestedTopology.RETRY_QUEUE,
                EmailVerificationRequestedTopology.MAX_RETRIES);
    }
}
