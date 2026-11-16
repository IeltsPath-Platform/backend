package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.library.VocabularyReviewDueTopology;
import com.ieltspath.events.messaging.MessageRetry;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VocabularyReviewDueMessagingConfig {
    public static final String CONTAINER_FACTORY = "vocabularyReviewDueListenerContainerFactory";
    public static final String RETRY = "vocabularyReviewDueRetry";

    @Bean Declarables vocabularyReviewDueTopology() { return VocabularyReviewDueTopology.declarables(); }

    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory vocabularyReviewDueListenerContainerFactory(ConnectionFactory connectionFactory) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean(RETRY)
    MessageRetry vocabularyReviewDueRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, VocabularyReviewDueTopology.RETRY_QUEUE, VocabularyReviewDueTopology.MAX_RETRIES);
    }
}
