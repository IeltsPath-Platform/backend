package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.UserEmailVerificationStateTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserEmailVerificationMessagingConfig {
    public static final String CONTAINER_FACTORY = "userEmailVerificationListenerContainerFactory";
    public static final String RETRY = "userEmailVerificationRetry";

    @Bean Declarables userEmailVerificationStateTopology() { return UserEmailVerificationStateTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory userEmailVerificationListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry userEmailVerificationRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, UserEmailVerificationStateTopology.RETRY_QUEUE,
                UserEmailVerificationStateTopology.MAX_RETRIES);
    }
}
