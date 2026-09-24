package com.group01.assessment.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Assessment owns the exchange it publishes to; consumers declare and bind their own queues.
 */
@Configuration
@EnableScheduling
public class AssessmentMessagingConfiguration {

    @Bean
    TopicExchange assessmentEventsExchange(
            @Value("${assessment.messaging.exchange:assessment.events}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }
}
