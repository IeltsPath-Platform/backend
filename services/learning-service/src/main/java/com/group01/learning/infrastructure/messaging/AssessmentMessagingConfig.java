package com.group01.learning.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology owned by the AssessmentCompleted.v2 consumer. A rejected delivery dead-letters to the retry queue, which
 * returns it to the main queue after a delay; contract violations and exhausted retries are parked in the DLQ.
 */
@Configuration
public class AssessmentMessagingConfig {
    public static final String EXCHANGE = "assessment.events";
    public static final String ROUTING_KEY = "assessment.completed.v2";
    public static final String QUEUE = "learning.assessment-completed.v2";
    public static final String RETRY_EXCHANGE = "learning.assessment-completed.retry";
    public static final String RETRY_QUEUE = "learning.assessment-completed.v2.retry";
    public static final String DEAD_LETTER_EXCHANGE = "learning.assessment-completed.dlx";
    public static final String DEAD_LETTER_QUEUE = "learning.assessment-completed.v2.dlq";

    @Bean
    Declarables assessmentCompletedTopology(@Value("${learning.messaging.retry-delay-ms:30000}") long retryDelayMs) {
        // Same attributes as Assessment's own declaration (durable, not auto-deleted).
        TopicExchange events = new TopicExchange(EXCHANGE, true, false);
        DirectExchange retry = new DirectExchange(RETRY_EXCHANGE, true, false);
        DirectExchange deadLetter = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue main = QueueBuilder.durable(QUEUE)
                .deadLetterExchange(RETRY_EXCHANGE).deadLetterRoutingKey(RETRY_QUEUE).build();
        Queue retryQueue = QueueBuilder.durable(RETRY_QUEUE).ttl((int) retryDelayMs)
                .deadLetterExchange("").deadLetterRoutingKey(QUEUE).build();
        Queue parked = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        Binding mainBinding = BindingBuilder.bind(main).to(events).with(ROUTING_KEY);
        Binding retryBinding = BindingBuilder.bind(retryQueue).to(retry).with(RETRY_QUEUE);
        Binding parkedBinding = BindingBuilder.bind(parked).to(deadLetter).with(DEAD_LETTER_QUEUE);
        return new Declarables(events, retry, deadLetter, main, retryQueue, parked, mainBinding, retryBinding,
                parkedBinding);
    }
}
