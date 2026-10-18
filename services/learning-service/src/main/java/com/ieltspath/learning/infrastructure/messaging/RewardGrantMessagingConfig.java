package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.events.points.PointsGrantMessaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology of the points grant exchange shared with Access. Learning sends {@code points.grant.requested}; Access
 * answers with {@code points.grant.settled}, which this service reads from its own queue.
 *
 * <p>An answer the consumer refuses (unreadable, or a failure that kept coming back) is dead-lettered to
 * {@link #SETTLED_DLQ}; a failure that may pass is parked on {@link #SETTLED_RETRY_QUEUE} and returns to
 * {@link #SETTLED_QUEUE} after {@link PointsGrantMessaging#RETRY_DELAY_MS}. Queue arguments cannot change once a queue exists: a broker that
 * already holds {@code learning.points-grant-settled} declared without the dead-letter arguments answers
 * {@code PRECONDITION_FAILED} until that queue is deleted.
 */
@Configuration
public class RewardGrantMessagingConfig {
    public static final String SETTLED_QUEUE = "learning.points-grant-settled";
    public static final String SETTLED_RETRY_QUEUE = "learning.points-grant-settled.retry";
    public static final String SETTLED_DLQ = "learning.points-grant-settled.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "learning.points-grant-settled.dlx";
    public static final String CONTAINER_FACTORY = "pointsGrantSettledListenerContainerFactory";

    @Bean
    Declarables pointsGrantTopology() {
        // Same attributes as Access's own declaration (durable, not auto-deleted).
        TopicExchange exchange = new TopicExchange(PointsGrantMessaging.EXCHANGE, true, false);
        Queue settled = QueueBuilder.durable(SETTLED_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(SETTLED_DLQ)
                .build();
        Binding binding = BindingBuilder.bind(settled).to(exchange).with(PointsGrantMessaging.SETTLED_ROUTING_KEY);

        DirectExchange deadLetterExchange = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue deadLetters = QueueBuilder.durable(SETTLED_DLQ).build();
        Binding deadLetterBinding = BindingBuilder.bind(deadLetters).to(deadLetterExchange).with(SETTLED_DLQ);

        // Holds a message for the retry delay, then hands it back to the settled queue through the default exchange.
        Queue retry = QueueBuilder.durable(SETTLED_RETRY_QUEUE)
                .ttl((int) PointsGrantMessaging.RETRY_DELAY_MS)
                .deadLetterExchange("")
                .deadLetterRoutingKey(SETTLED_QUEUE)
                .build();
        return new Declarables(exchange, settled, binding, deadLetterExchange, deadLetters, deadLetterBinding, retry);
    }

    /** A refused message is dead-lettered, never put back on the queue where it would be redelivered at once. */
    @Bean(CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory pointsGrantSettledListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    MessageRetry pointsGrantSettledRetry(RabbitTemplate rabbitTemplate) {
        return new MessageRetry(rabbitTemplate, SETTLED_RETRY_QUEUE, PointsGrantMessaging.MAX_RETRIES);
    }
}
