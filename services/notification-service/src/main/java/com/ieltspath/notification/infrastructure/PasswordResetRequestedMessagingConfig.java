package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.PasswordResetRequestedTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordResetRequestedMessagingConfig {
    public static final String CONTAINER_FACTORY = "passwordResetListenerContainerFactory";
    public static final String RETRY = "passwordResetRetry";
    @Bean Declarables passwordResetRequestedTopology() { return PasswordResetRequestedTopology.declarables(); }
    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory passwordResetListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory); factory.setDefaultRequeueRejected(false); return factory;
    }
    @Bean(RETRY)
    MessageRetry passwordResetRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, PasswordResetRequestedTopology.RETRY_QUEUE,
                PasswordResetRequestedTopology.MAX_RETRIES);
    }
}
