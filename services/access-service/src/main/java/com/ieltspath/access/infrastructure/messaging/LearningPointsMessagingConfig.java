package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.events.points.PointsGrantSettled;

import com.ieltspath.events.points.PointsGrantMessaging;
import com.ieltspath.events.messaging.RetryTopology;
import com.ieltspath.events.messaging.MessageRetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Declarables;
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
 * {@link #GRANT_RETRY_QUEUE} and returns to {@link #GRANT_QUEUE} after {@link PointsGrantMessaging#RETRY_DELAY_MS}.
 *
 * <p>Queue arguments cannot change once a queue exists: a broker that already holds {@code access.points-grant} declared
 * without the dead-letter arguments answers {@code PRECONDITION_FAILED} until that queue is deleted.
 */
@Configuration
public class LearningPointsMessagingConfig {
    public static final String GRANT_QUEUE = "access.points-grant";
    public static final String GRANT_RETRY_QUEUE = "access.points-grant.retry";
    public static final String GRANT_DLQ = "access.points-grant.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "access.points-grant.dlx";
    public static final String CONTAINER_FACTORY = "pointsGrantListenerContainerFactory";

    @Bean
    Declarables pointsGrantTopology() {
        TopicExchange exchange = new TopicExchange(PointsGrantMessaging.EXCHANGE, true, false);
        return RetryTopology.of(new RetryTopology.Names(GRANT_QUEUE, GRANT_RETRY_QUEUE, GRANT_DLQ, DEAD_LETTER_EXCHANGE),
                PointsGrantMessaging.RETRY_DELAY_MS, exchange, PointsGrantMessaging.REQUESTED_ROUTING_KEY);
    }

    @Bean
    MessageConverter pointsMessageConverter(ObjectMapper objectMapper) {
        var converter = new Jackson2JsonMessageConverter(objectMapper);
        var classMapper = new org.springframework.amqp.support.converter.DefaultClassMapper();
        // Preserve the wire type id used before the payload moved to the shared module.
        classMapper.setIdClassMapping(java.util.Map.of(
                "com.ieltspath.access.infrastructure.messaging.PointsGrantSettled", PointsGrantSettled.class));
        classMapper.afterPropertiesSet();
        converter.setClassMapper(classMapper);
        return converter;
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
        return new MessageRetry(rabbitTemplate, GRANT_RETRY_QUEUE, PointsGrantMessaging.MAX_RETRIES);
    }
}
