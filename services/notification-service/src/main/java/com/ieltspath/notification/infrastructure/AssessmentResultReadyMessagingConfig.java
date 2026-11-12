package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.messaging.MessageRetry;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssessmentResultReadyMessagingConfig {
    public static final String CONTAINER_FACTORY = "assessmentResultReadyListenerContainerFactory";
    public static final String RETRY = "assessmentResultReadyRetry";

    @Bean Declarables assessmentResultReadyTopology() { return AssessmentResultReadyTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory assessmentResultReadyListenerContainerFactory(ConnectionFactory connectionFactory) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry assessmentResultReadyRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, AssessmentResultReadyTopology.RETRY_QUEUE,
                AssessmentResultReadyTopology.MAX_RETRIES);
    }
}
