package com.ieltspath.events.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryTopologyTest {
    @Test
    void requestedGrantKeepsItsBrokerArguments() {
        assertTopology("access.points-grant", "access.points-grant.retry", "access.points-grant.dlq",
                "access.points-grant.dlx", "points.grant.requested");
    }

    @Test
    void settledGrantKeepsItsBrokerArguments() {
        assertTopology("learning.points-grant-settled", "learning.points-grant-settled.retry",
                "learning.points-grant-settled.dlq", "learning.points-grant-settled.dlx", "points.grant.settled");
    }

    private static void assertTopology(String queue, String retryQueue, String dlq, String dlx, String routingKey) {
        TopicExchange source = new TopicExchange("learning.points", true, false);
        Declarables topology = RetryTopology.of(new RetryTopology.Names(queue, retryQueue, dlq, dlx),
                30_000L, source, routingKey);
        assertEquals(7, topology.getDeclarables().size());
        Map<String, Queue> queues = topology.getDeclarablesByType(Queue.class).stream()
                .collect(Collectors.toMap(Queue::getName, q -> q));
        assertEquals(Set.of(queue, retryQueue, dlq), queues.keySet());
        assertEquals(Map.of("x-dead-letter-exchange", dlx, "x-dead-letter-routing-key", dlq),
                queues.get(queue).getArguments());
        assertEquals(Map.of("x-message-ttl", 30000, "x-dead-letter-exchange", "",
                "x-dead-letter-routing-key", queue), queues.get(retryQueue).getArguments());
        assertEquals(Map.of(), queues.get(dlq).getArguments());
        queues.values().forEach(q -> {
            assertTrue(q.isDurable());
            assertFalse(q.isAutoDelete());
            assertFalse(q.isExclusive());
        });
        assertSame(source, topology.getDeclarablesByType(TopicExchange.class).getFirst());
        assertTrue(source.isDurable());
        assertFalse(source.isAutoDelete());
        DirectExchange deadLetterExchange = topology.getDeclarablesByType(DirectExchange.class).getFirst();
        assertEquals(dlx, deadLetterExchange.getName());
        assertTrue(deadLetterExchange.isDurable());
        assertFalse(deadLetterExchange.isAutoDelete());
        var bindings = topology.getDeclarablesByType(Binding.class);
        assertEquals(2, bindings.size());
        assertBinding(bindings.get(0), queue, "learning.points", routingKey);
        assertBinding(bindings.get(1), dlq, dlx, dlq);
    }

    private static void assertBinding(Binding binding, String destination, String exchange, String key) {
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
        assertEquals(destination, binding.getDestination());
        assertEquals(exchange, binding.getExchange());
        assertEquals(key, binding.getRoutingKey());
        assertTrue(binding.getArguments().isEmpty());
    }
}
