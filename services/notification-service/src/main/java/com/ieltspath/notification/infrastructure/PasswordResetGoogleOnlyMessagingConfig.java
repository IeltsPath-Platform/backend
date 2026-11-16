package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.PasswordResetGoogleOnlyTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordResetGoogleOnlyMessagingConfig {
    public static final String CONTAINER_FACTORY = "passwordResetGoogleOnlyListenerContainerFactory";
    public static final String RETRY = "passwordResetGoogleOnlyRetry";

    @Bean Declarables passwordResetGoogleOnlyTopology() { return PasswordResetGoogleOnlyTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory passwordResetGoogleOnlyListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry passwordResetGoogleOnlyRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, PasswordResetGoogleOnlyTopology.RETRY_QUEUE, PasswordResetGoogleOnlyTopology.MAX_RETRIES);
    }
}
