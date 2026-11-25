package com.ieltspath.assessment.infrastructure.messaging;

import com.ieltspath.events.assessment.AssessmentMessaging;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlacementSpeakingCompletedMessagingConfig {
    public static final String QUEUE = "assessment.placement-speaking-grading.completed.v1";
    public static final String RETRY_EXCHANGE = "assessment.placement-speaking-grading.retry";
    public static final String RETRY_QUEUE = "assessment.placement-speaking-grading.completed.v1.retry";
    public static final String DLX = "assessment.placement-speaking-grading.dlx";
    public static final String DLQ = "assessment.placement-speaking-grading.completed.v1.dlq";

    @Bean
    Declarables placementSpeakingCompletedTopology(@Value("${assessment.placement.speaking-grading.retry-delay-ms:30000}") long retryDelay) {
        TopicExchange events = new TopicExchange(AssessmentMessaging.EXCHANGE, true, false);
        DirectExchange retry = new DirectExchange(RETRY_EXCHANGE, true, false);
        DirectExchange dead = new DirectExchange(DLX, true, false);
        Queue main = QueueBuilder.durable(QUEUE).deadLetterExchange(RETRY_EXCHANGE).deadLetterRoutingKey(RETRY_QUEUE).build();
        Queue retryQueue = QueueBuilder.durable(RETRY_QUEUE).ttl((int) retryDelay).deadLetterExchange("").deadLetterRoutingKey(QUEUE).build();
        Queue dlq = QueueBuilder.durable(DLQ).build();
        return new Declarables(events, retry, dead, main, retryQueue, dlq,
                BindingBuilder.bind(main).to(events).with(AssessmentMessaging.SPEAKING_GRADING_COMPLETED_ROUTING_KEY),
                BindingBuilder.bind(retryQueue).to(retry).with(RETRY_QUEUE),
                BindingBuilder.bind(dlq).to(dead).with(DLQ));
    }
}
