package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.events.assessment.AssessmentMessaging;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlacementWritingRequestedMessagingConfig {
    public static final String QUEUE = "learning.placement-writing-grading.requested.v1";
    public static final String RETRY_EXCHANGE = "learning.placement-writing-grading.retry";
    public static final String RETRY_QUEUE = "learning.placement-writing-grading.requested.v1.retry";
    public static final String DLX = "learning.placement-writing-grading.dlx";
    public static final String DLQ = "learning.placement-writing-grading.requested.v1.dlq";

    @Bean
    Declarables placementWritingRequestedTopology(@Value("${learning.placement-writing.retry-delay-ms:30000}") long retryDelay) {
        TopicExchange events = new TopicExchange(AssessmentMessaging.EXCHANGE, true, false);
        DirectExchange retry = new DirectExchange(RETRY_EXCHANGE, true, false);
        DirectExchange dead = new DirectExchange(DLX, true, false);
        Queue main = QueueBuilder.durable(QUEUE).deadLetterExchange(RETRY_EXCHANGE).deadLetterRoutingKey(RETRY_QUEUE).build();
        Queue retryQueue = QueueBuilder.durable(RETRY_QUEUE).ttl((int) retryDelay).deadLetterExchange("").deadLetterRoutingKey(QUEUE).build();
        Queue dlq = QueueBuilder.durable(DLQ).build();
        return new Declarables(events,retry,dead,main,retryQueue,dlq,
                BindingBuilder.bind(main).to(events).with(AssessmentMessaging.PLACEMENT_WRITING_REQUESTED_ROUTING_KEY),
                BindingBuilder.bind(retryQueue).to(retry).with(RETRY_QUEUE),
                BindingBuilder.bind(dlq).to(dead).with(DLQ));
    }
}
