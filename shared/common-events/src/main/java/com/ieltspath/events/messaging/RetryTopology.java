package com.ieltspath.events.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

/**
 * Builds the retry topology (main queue, retry queue, dead-letter queue and exchange) for the two points grant queues and the
 * four grading notice queues. The names of the two grant queues stay owned by their services; the names of the four grading
 * queues live in the {@code Grading*Topology} classes of this library.
 */
public final class RetryTopology {
    public record Names(String queue, String retryQueue, String deadLetterQueue, String deadLetterExchange) { }

    private RetryTopology() { }

    /**
     * The main queue dead-letters to the DLQ through its own direct DLX; the retry queue holds a message for
     * {@code retryDelayMs}, then hands it back to the main queue through the default exchange.
     */
    public static Declarables of(Names n, long retryDelayMs, Exchange sourceExchange, String routingKey) {
        Queue main = QueueBuilder.durable(n.queue())
                .deadLetterExchange(n.deadLetterExchange())
                .deadLetterRoutingKey(n.deadLetterQueue())
                .build();
        Queue retry = QueueBuilder.durable(n.retryQueue())
                .ttl((int) retryDelayMs)
                .deadLetterExchange("")
                .deadLetterRoutingKey(n.queue())
                .build();
        Queue deadLetters = QueueBuilder.durable(n.deadLetterQueue()).build();
        DirectExchange deadLetterExchange = new DirectExchange(n.deadLetterExchange(), true, false);
        Binding binding = new Binding(n.queue(), Binding.DestinationType.QUEUE,
                sourceExchange.getName(), routingKey, null);
        Binding deadLetterBinding = new Binding(n.deadLetterQueue(), Binding.DestinationType.QUEUE,
                n.deadLetterExchange(), n.deadLetterQueue(), null);
        return new Declarables(sourceExchange, main, binding, deadLetterExchange, deadLetters, deadLetterBinding, retry);
    }
}
