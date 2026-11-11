package com.ieltspath.notification.infrastructure;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression(LearningMailGate.EXPRESSION)
public class LearningMailMessagingConfig {
    @Bean Declarables learningMailTopology() { return LearningMailTopology.declarables(); }

    @Bean(LearningMailTopology.CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory learningMailListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
