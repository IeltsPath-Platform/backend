package com.ieltspath.learning.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology of the points grant exchange shared with Access. Learning sends {@code points.grant.requested}; Access
 * answers with {@code points.grant.settled}, which this service reads from its own queue.
 */
@Configuration
public class RewardGrantMessagingConfig {
    public static final String EXCHANGE = "learning.points";
    public static final String REQUESTED_ROUTING_KEY = "points.grant.requested";
    public static final String SETTLED_ROUTING_KEY = "points.grant.settled";
    public static final String SETTLED_QUEUE = "learning.points-grant-settled";

    @Bean
    Declarables pointsGrantTopology() {
        // Same attributes as Access's own declaration (durable, not auto-deleted).
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        Queue settled = new Queue(SETTLED_QUEUE, true);
        Binding binding = BindingBuilder.bind(settled).to(exchange).with(SETTLED_ROUTING_KEY);
        return new Declarables(exchange, settled, binding);
    }
}
