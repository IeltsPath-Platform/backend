package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.GoogleAccountUnlinkedTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAccountUnlinkedMessagingConfig {
    public static final String CONTAINER_FACTORY = "googleAccountUnlinkedListenerContainerFactory";
    public static final String RETRY = "googleAccountUnlinkedRetry";

    @Bean Declarables googleAccountUnlinkedTopology() { return GoogleAccountUnlinkedTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory googleAccountUnlinkedListenerContainerFactory(ConnectionFactory factory) {
        SimpleRabbitListenerContainerFactory listener = new SimpleRabbitListenerContainerFactory();
        listener.setConnectionFactory(factory);
        listener.setDefaultRequeueRejected(false);
        return listener;
    }

    @Bean(RETRY)
    MessageRetry googleAccountUnlinkedRetry(RabbitTemplate template) {
        return new MessageRetry(template, GoogleAccountUnlinkedTopology.RETRY_QUEUE, GoogleAccountUnlinkedTopology.MAX_RETRIES);
    }
}
