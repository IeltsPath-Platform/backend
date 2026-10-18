package com.ieltspath.access.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology of the points grant queue. A message the consumer refuses (bad payload, constraint violation, or a failure
 * that kept coming back) is dead-lettered to {@link #GRANT_DLQ}; a failure that may pass is parked on
 * {@link #GRANT_RETRY_QUEUE} and returns to {@link #GRANT_QUEUE} after {@link #RETRY_DELAY_MS}.
 *
 * <p>Queue arguments cannot change once a queue exists: a broker that already holds {@code access.points-grant} declared
 * without the dead-letter arguments answers {@code PRECONDITION_FAILED} until that queue is deleted.
 */
@Configuration
public class LearningPointsMessagingConfig {
    public static final String EXCHANGE = "learning.points";
    public static final String GRANT_QUEUE = "access.points-grant";
    public static final String GRANT_RETRY_QUEUE = "access.points-grant.retry";
    public static final String GRANT_DLQ = "access.points-grant.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "access.points-grant.dlx";
    public static final String GRANT_ROUTING_KEY = "points.grant.requested";
    public static final String SETTLED_ROUTING_KEY = "points.grant.settled";
    public static final String CONTAINER_FACTORY = "pointsGrantListenerContainerFactory";
    public static final long RETRY_DELAY_MS = 30_000;
    public static final int MAX_RETRIES = 5;

    @Bean
    TopicExchange learningPointsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange accessPointsGrantDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue accessPointsGrantQueue() {
        return QueueBuilder.durable(GRANT_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(GRANT_DLQ)
                .build();
    }

    @Bean
    Queue accessPointsGrantDeadLetterQueue() {
        return QueueBuilder.durable(GRANT_DLQ).build();
    }

    /** Holds a message for the retry delay, then hands it back to the grant queue through the default exchange. */
    @Bean
    Queue accessPointsGrantRetryQueue() {
        return QueueBuilder.durable(GRANT_RETRY_QUEUE)
                .ttl((int) RETRY_DELAY_MS)
                .deadLetterExchange("")
                .deadLetterRoutingKey(GRANT_QUEUE)
                .build();
    }

    @Bean
    Binding accessPointsGrantBinding() {
        return BindingBuilder.bind(accessPointsGrantQueue()).to(learningPointsExchange()).with(GRANT_ROUTING_KEY);
    }

    @Bean
    Binding accessPointsGrantDeadLetterBinding() {
        return BindingBuilder.bind(accessPointsGrantDeadLetterQueue()).to(accessPointsGrantDeadLetterExchange())
                .with(GRANT_DLQ);
    }

    @Bean
    MessageConverter pointsMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** A refused message is dead-lettered, never put back on the queue where it would be redelivered at once. */
    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory pointsGrantListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    MessageRetry pointsGrantRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, GRANT_RETRY_QUEUE, MAX_RETRIES);
    }
}
