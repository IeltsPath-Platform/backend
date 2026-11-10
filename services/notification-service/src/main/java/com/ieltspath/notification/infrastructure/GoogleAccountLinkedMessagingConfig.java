package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.GoogleAccountLinkedTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAccountLinkedMessagingConfig {
    public static final String CONTAINER_FACTORY = "googleAccountLinkedListenerContainerFactory";
    public static final String RETRY = "googleAccountLinkedRetry";

    @Bean Declarables googleAccountLinkedTopology() { return GoogleAccountLinkedTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory googleAccountLinkedListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry googleAccountLinkedRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, GoogleAccountLinkedTopology.RETRY_QUEUE, GoogleAccountLinkedTopology.MAX_RETRIES);
    }
}
