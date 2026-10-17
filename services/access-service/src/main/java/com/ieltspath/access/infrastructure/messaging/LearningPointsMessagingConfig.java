package com.ieltspath.access.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LearningPointsMessagingConfig {
    public static final String EXCHANGE = "learning.points";
    public static final String GRANT_QUEUE = "access.points-grant";
    public static final String GRANT_ROUTING_KEY = "points.grant.requested";
    public static final String SETTLED_ROUTING_KEY = "points.grant.settled";

    @Bean
    TopicExchange learningPointsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue accessPointsGrantQueue() {
        return new Queue(GRANT_QUEUE, true);
    }

    @Bean
    Binding accessPointsGrantBinding(Queue accessPointsGrantQueue, TopicExchange learningPointsExchange) {
        return BindingBuilder.bind(accessPointsGrantQueue).to(learningPointsExchange).with(GRANT_ROUTING_KEY);
    }

    @Bean
    MessageConverter pointsMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
